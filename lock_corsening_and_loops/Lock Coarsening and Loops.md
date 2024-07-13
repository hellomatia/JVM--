# Lock Coarsening and Loops

## Lock Coarsening

### Lock Coarsening 이란?
Lock Coarsening은 JVM에서 사용하는 성능 최적화 기법 중 하나이다. 여러 개의 작은 동기화 블록을 하나의 큰 동기화 블록으로 합치는 과정이다.
```java
synchronized (obj) {
    // statements 1
}
synchronized (obj) {
    // statements 2
}
```
위 코드를 다음과 같이 최적화를 진행한다.
```java 
synchronized (obj) {
    // statements 1
    // statements 2
}
```
이를 통해 락킹 오버헤드를 줄일 수 있다.

### Loop에도 Lock Coarsening이 적용될까?
```java 
for (...) {
  synchronized (obj) {
    // something
  }
}
```
위 코드가 다음과 같이 최적화가 진행될까?
```java 
synchronized (this) {
  for (...) {
     // something
  }
}
```
가능은 하지만, 이렇게 실행되면 스레드가 큰 루프를 실행하는 동안 락을 독점할 수 있는 단점이 있다.

### 확인해보기
```java 
    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void test() {
        for (int c = 0; c < 1000; c++) {
            synchronized (this) {
                x += 0x42;
            }
        }
    }
```
JMH툴을 사용하여 벤치마킹 테스트를 진행하였다.
밴치마킹 성능을 살펴보았더니 다음과 같은 결과가 나왔다.

**테스트 환경**

| Window11, intel i5, JDK 1.8.0_332
```
Benchmark       Mode  Cnt     Score    Error  Units
LockRoach.test  avgt    5  4015.255 ± 69.875  ns/op
```
이 숫자로는 무엇인지 판단할 수 없다. 그럼 실제로 JVM이 어떻게 동작하는지 살펴보자. `-prof perfasm`은 JMH의 프로파일링 옵션으로 생성된 코드에서 가장 많이 실행되는 부분을 보여준다. 처음 보면 무서운 출력이 나온다고 한다.(시도했지만 Window환경이라 실패했다. 다시 Linux환경에서 실행할 계획이다.)

실제 글 저자의 어셈블리 코드를 확인해보자.
```asm
↗  0x00007f455cc708c1: lea    0x20(%rsp),%rbx
 │          < 어쩌고저쩌고, 모니터 진입 >     ; <--- 락을 확득하는 코드
 │  0x00007f455cc70918: mov    (%rsp),%r10        ; $this 로드
 │  0x00007f455cc7091c: mov    0xc(%r10),%r11d    ; $this.x 로드
 │  0x00007f455cc70920: mov    %r11d,%r10d        ; ...흠...
 │  0x00007f455cc70923: add    $0x42,%r10d        ; ...흠흠...
 │  0x00007f455cc70927: mov    (%rsp),%r8         ; ...흠흠흠!...
 │  0x00007f455cc7092b: mov    %r10d,0xc(%r8)     ; Hotspot, 중복 저장, 두 줄 아래에서 무효화됨
 │  0x00007f455cc7092f: add    $0x108,%r11d       ; 0x108 = 0x42 * 4 더하기 <-- 4번 언롤링됨
 │  0x00007f455cc70936: mov    %r11d,0xc(%r8)     ; $this.x 다시 저장
 │          < 어쩌고저쩌고, 모니터 종료 >      ; <--- 락을 해제하는 코드
 │  0x00007f455cc709c6: add    $0x4,%ebp          ; c += 4   <--- 4번 언롤링됨
 │  0x00007f455cc709c9: cmp    $0x3e5,%ebp        ; c < 1000?
 ╰  0x00007f455cc709cf: jl     0x00007f455cc708c1
```
어셈블리어 코드를 확인해보면, 락 획득과 해제가 언롤링된 4번의 반복을 감싸고 있다. 이는 제한된 범위 내에서 Lock coarsening이 적용되었음을 의미한다. 완전히 루프 전체를 감싸는 것이 아니라, 언롤링된 4번의 반복 내에서 락 거칠기 완화가 이루어져있다.
`-XX:LoopUnrollLimit=1`설정을 하여 어떤 성능 이점을 얻었는지 확인해보자.
```
Benchmark       Mode  Cnt     Score    Error  Units

# Defalt
LockRoach.test  avgt    5  4015.255 ± 69.875  ns/op

# LoopUnrollLimit=1
LockRoach.test  avgt    5  15816.794 ± 249.303  ns/op
```
약 4배정도의 성능 차이를 확인할 수 있다.
그렇다면 'LoopUnrollLimit=1'옵션이 적용된 어셈블리어 코드를 확인하자.

```java
 ↗  0x00007f964d0893d2: lea    0x20(%rsp),%rbx
 │          < 어쩌고저쩌고, 모니터 진입 >
 │  0x00007f964d089429: mov    (%rsp),%r10        ; $this 로드
 │  0x00007f964d08942d: addl   $0x42,0xc(%r10)    ; $this.x += 0x42
 │          < 어쩌고저쩌고, 모니터 종료 >
 │  0x00007f964d0894be: inc    %ebp               ; c++
 │  0x00007f964d0894c0: cmp    $0x3e8,%ebp        ; c < 1000?
 ╰  0x00007f964d0894c6: jl     0x00007f964d0893d2 ;
```
모든게 확인 되었다. 끝!

추가로 처음테스트 할때, Amazon사의 corretto-17.0.9사용했으나, 'LoopUnrollLimit=1'옵션 적용하여도 성능이 똑같았다. 언롤링 최적화가 이루어지지 않은듯 하다.

### References
- [JVM Anatomy Quark #1: Lock Coarsening and Loops](https://shipilev.net/jvm/anatomy-quarks/1-lock-coarsening-for-loops/)