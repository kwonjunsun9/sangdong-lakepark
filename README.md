# 상동 호수공원 어디로 가요?

상동 호수공원을 배경으로 장애물을 피하고 아이템을 모으는 2D 플랫포머 게임입니다.
Java와 libGDX로 제작했으며, Easy → Normal → Hard → Extreme 순서로 스테이지가 열립니다.
Extreme을 클리어하면 엔딩을 보고 새 게임을 시작할 수 있습니다.

## 실행 환경

- Windows, macOS 또는 Linux의 그래픽 데스크톱 환경
- **64비트 JDK 17 권장** (JDK 21에서도 소스 빌드 가능)
- 처음 빌드할 때 인터넷 연결 필요

Gradle은 따로 설치하지 않아도 됩니다. 저장소에 포함된 Gradle Wrapper가 빌드 도구와 의존성을 내려받습니다.
`java -version`과 `javac -version`으로 JDK 설치를 확인하고, `JAVA_HOME`이 같은 JDK 폴더를 가리키도록 설정하세요.

## 소스에서 실행

저장소를 클론한 뒤 `gradlew`와 `assets`가 있는 프로젝트 최상위 폴더에서 실행합니다.
이미지·음원·폰트는 저장소에 포함되어 있어 별도로 받을 필요가 없습니다.

Windows PowerShell:

```powershell
.\gradlew.bat lwjgl3:run
```

Windows에서는 `run-game.bat`를 더블클릭해도 됩니다.

macOS / Linux:

```bash
./gradlew lwjgl3:run
```

IntelliJ IDEA에서는 프로젝트 최상위 폴더를 Gradle 프로젝트로 열고,
실행 구성 **상동 호수공원 어디로 가요?** 를 선택합니다.

## 조작 방법

| 화면 | 조작 |
| --- | --- |
| 시작 메뉴 | ↑ / ↓ 선택, Space / Enter 확인, 마우스 클릭 |
| 스테이지 지도 | ← / → 스테이지 선택, Space / Enter 입장 |
| 플레이 | ← / → 이동, Space 점프 |
| 더블 점프 | 관련 아이템 획득 후 공중에서 Space |
| 대화 | ← 이전, → / Space / Enter 다음 |
| 설정·뒤로가기·일시정지 | Esc |

설정에서 소리를 켜거나 끌 수 있습니다. 스테이지 해금 진행도와 소리 설정은 사용자 PC에 저장됩니다.

## 테스트와 JAR 빌드

Windows:

```powershell
.\gradlew.bat clean core:test lwjgl3:jar
java -jar lwjgl3/build/libs/sangdong-lakepark-1.0.0.jar
```

macOS / Linux:

```bash
./gradlew clean core:test lwjgl3:jar
java -jar lwjgl3/build/libs/sangdong-lakepark-1.0.0.jar
```

macOS에서 JAR를 직접 실행할 때는 `java -XstartOnFirstThread -jar ...`를 사용하세요.
JAR에는 게임 리소스와 라이브러리가 포함됩니다. JAR 실행에는 Java가 필요합니다.

## Windows 배포

Java 런타임을 포함한 `.exe` 배포본은 다음 명령으로 만듭니다.

```powershell
.\gradlew.bat lwjgl3:packageWinX64
```


## 프로젝트 구성

- `core/`: 게임 화면, 스테이지, 물리, 사운드와 테스트
- `lwjgl3/`: 데스크톱 실행 및 배포 설정
- `assets/`: 게임에서 사용하는 이미지, 음원, 폰트와 화면 배치
- `gradle/`, `gradlew`, `gradlew.bat`: Gradle Wrapper

## 사용 기술 및 출처

- [libGDX](https://libgdx.com/), LWJGL3, Gradle, [Construo](https://github.com/fourlastor-alexandria/construo)
- 프로젝트 기반: [gdx-liftoff](https://github.com/libgdx/gdx-liftoff)
- 게임 이미지: AI 생성 이미지 포함
- 영상 출처: 게임 소개 화면의 출처 링크
- NanumGothic Bold: [SIL Open Font License](assets/fonts/OFL.txt)
- Pretendard JP: [SIL Open Font License](assets/fonts/pretendard-jp/LICENSE.txt)
