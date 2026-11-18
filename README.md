# SPRING_BOOT_20220672

자바웹프로그래밍(2) 수업 실습 정리

- 학번: 20220672
- 개발 환경: Java 25, Spring Boot 4.1.1, MySQL 8
- 사용 기술: Spring Data JPA, Thymeleaf, Spring Security, Bootstrap 5

[7주차](#week7) · [9주차](#week9) · [10주차](#week10) · [11주차](#week11) · [실행 방법](#실행-방법)

## 1~2주차 - 개발 환경 설정

- JDK와 Spring Boot 프로젝트 설정
- `DemoApplication` 실행 후 8080 포트 접속
- `/hello` 주소 연결
- 컨트롤러에서 Model로 값을 넘기고 Thymeleaf로 출력

관련 파일: [DemoApplication.java](src/main/java/com/example/demo/DemoApplication.java), [DemoController.java](src/main/java/com/example/demo/controller/DemoController.java), [hello.html](src/main/resources/templates/hello.html)

## 3주차 - 포트폴리오 화면

- TemplateMo First Portfolio 템플릿 적용
- CSS, JS, 이미지, 폰트를 `static` 폴더에 배치
- 소개, 기술, 프로젝트, 연락처 메뉴 구성
- Thymeleaf로 정적 파일 경로 연결
- 웹, AI, 보안, 게임 상세 페이지 추가

관련 파일: [index.html](src/main/resources/templates/index.html), [웹](src/main/resources/public/detailed_web.html), [AI](src/main/resources/static/detailed_ai.html), [보안](src/main/resources/static/detailed_security.html), [게임](src/main/resources/static/detailed_game.html)

## 4주차 - MySQL 연동

- Spring Data JPA와 MySQL 드라이버 추가
- `localhost:3306/spring` DB 연결
- `TestDB` 엔티티와 Repository, Service 작성
- `/testdb`에서 DB에 저장된 목록 출력
- 연습문제: 나이(`age`), 성별(`gender`) 컬럼 추가

처리 순서: Controller → Service → Repository → DB

관련 파일: [TestDB.java](src/main/java/com/example/demo/model/domain/TestDB.java), [TestRepository.java](src/main/java/com/example/demo/model/repository/TestRepository.java), [TestService.java](src/main/java/com/example/demo/model/service/TestService.java), [testdb.html](src/main/resources/templates/testdb.html)

### SQL 실습

```sql
INSERT INTO testdb (id, name)
VALUES (1, '홍길동');
```

나이와 성별을 추가한 뒤 MySQL 확장에서 데이터 입력.

| 아이디 | 이름 | 나이 | 성별 |
| --- | --- | --- | --- |
| 1 | 홍길동 | 25 | 남 |
| 2 | 아저씨 | 40 | 남 |
| 3 | 김영희 | 30 | 여 |
| 4 | 이철수 | 22 | 남 |

## 5주차 - 회원가입과 로그인

- Spring Security와 Thymeleaf Security 의존성 추가
- 회원가입, 로그인, 로그아웃 구현
- 가입할 때 비밀번호를 BCrypt로 암호화해서 저장
- 아이디 중복과 필수 입력값 검사
- 로그인 여부에 따라 메뉴 표시 변경
- 로그아웃은 CSRF 토큰을 포함한 POST 요청으로 처리

### 연습문제

- 로그인 상태 유지: `remember-me` 쿠키 유효기간 7일 설정
- 비밀번호 확인: 두 입력값이 다르면 가입을 막고 오류 표시
- 가입 실패 시 아이디와 이름 입력값 유지

관련 파일: [SecurityConfig.java](src/main/java/com/example/demo/config/SecurityConfig.java), [MemberController.java](src/main/java/com/example/demo/controller/MemberController.java), [MemberService.java](src/main/java/com/example/demo/model/service/MemberService.java), [login.html](src/main/resources/templates/login.html), [signup.html](src/main/resources/templates/signup.html)

## 6주차 - 권한과 회원 관리

- USER, MANAGER, ADMIN 권한 구분
- `/mypage`에서 로그인한 회원 정보 조회
- 관리자 페이지에서 회원 목록 조회, 권한 변경, 회원 삭제
- 화면 메뉴와 서버 요청에서 각각 권한 확인
- 권한이 없으면 403 페이지 표시

| 사용자 | `/mypage` | `/testdb` | `/admin/**` |
| --- | --- | --- | --- |
| 비로그인 | 로그인 필요 | 로그인 필요 | 로그인 필요 |
| USER | 가능 | 403 | 403 |
| MANAGER | 가능 | 가능 | 403 |
| ADMIN | 가능 | 가능 | 가능 |

### 연습문제

- 관리자가 자기 권한을 바꾸거나 자기 계정을 삭제하지 못하도록 처리
- MANAGER 권한 추가
- 회원목록 메뉴는 ADMIN, MANAGER에게만 표시

관리자 계정은 회원가입 후 DB에서 권한 변경.

```sql
UPDATE member SET role = 'ADMIN' WHERE username = 'admin';
```

권한을 바꾼 뒤에는 로그아웃 후 다시 로그인해야 적용됨.

관련 파일: [AdminController.java](src/main/java/com/example/demo/controller/AdminController.java), [MemberService.java](src/main/java/com/example/demo/model/service/MemberService.java), [mypage.html](src/main/resources/templates/mypage.html), [members.html](src/main/resources/templates/admin/members.html)

<a id="week7"></a>

## 7주차 - REST API와 JSON

- 회원 정보를 JSON으로 반환하는 API 작성
- 응답 DTO인 `MemberResponse`에서 비밀번호 제외
- Basic Auth로 인증 처리
- 관리자 API에서 회원 조회, 등록, 권한 변경, 삭제
- `/admin/members-api` 화면에서 fetch로 API 호출

| 메서드 | 주소 | 기능 | 권한 |
| --- | --- | --- | --- |
| GET | `/api/hello` | 메시지와 시간 출력 | 누구나 |
| GET | `/api/me` | 내 정보 조회 | 로그인 |
| GET | `/api/admin/members` | 회원 목록·검색 | ADMIN |
| GET | `/api/admin/members/{id}` | 회원 1명 조회 | ADMIN |
| POST | `/api/admin/members` | 회원 등록 | ADMIN |
| PATCH | `/api/admin/members/{id}/role` | 권한 변경 | ADMIN |
| DELETE | `/api/admin/members/{id}` | 회원 삭제 | ADMIN |

### 연습문제

- `?keyword=student`로 아이디 또는 이름 검색
- `@RestControllerAdvice`로 관리자 API의 입력 오류 처리
- 잘못된 입력은 400과 오류 메시지 반환

### Postman 실습

Authorization에서 Basic Auth를 선택하고 가입한 계정으로 요청.

- 인증 없이 `/api/me` 요청: 401
- 일반 회원으로 관리자 API 요청: 403
- 없는 회원 조회: 404
- 회원 등록: 201, 응답 헤더에 Location 포함
- 권한 변경: 200
- 회원 삭제: 204

```json
{
  "username": "postman1",
  "password": "가입할_비밀번호",
  "passwordConfirm": "가입할_비밀번호",
  "name": "포스트맨"
}
```

권한 변경 요청 본문은 `{"role":"ADMIN"}`. 회원가입으로 생성되는 계정의 기본 권한은 USER.

관련 파일: [MemberApiController.java](src/main/java/com/example/demo/controller/MemberApiController.java), [AdminApiController.java](src/main/java/com/example/demo/controller/AdminApiController.java), [MemberResponse.java](src/main/java/com/example/demo/model/dto/MemberResponse.java), [ApiExceptionHandler.java](src/main/java/com/example/demo/controller/ApiExceptionHandler.java)

<a id="week9"></a>

## 9주차 - 게시판 CRUD

- `Post` 엔티티와 `posts` 테이블 생성
- 제목, 내용, 작성자, 작성 시간, 수정 시간, 조회수 필드 추가
- `@ManyToOne`으로 글과 작성자 연결
- 글 목록, 상세, 작성, 수정, 삭제 구현
- 작성자는 폼 입력값 대신 로그인 정보로 지정
- 제목과 내용이 비어 있거나 제목이 100자를 넘으면 저장 차단
- 없는 글은 404 처리

| 요청 | 기능 | 권한 |
| --- | --- | --- |
| `GET /board` | 목록 | 누구나 |
| `GET /board/{id}` | 상세 | 누구나 |
| `GET /board/write`, `POST /board/write` | 글쓰기 | 로그인 |
| `GET /board/{id}/edit`, `POST /board/{id}/edit` | 수정 | 작성자 |
| `POST /board/{id}/delete` | 삭제 | 작성자 또는 ADMIN |

### 오류 정리

`PostRepository`를 두 패키지에 넣어서 같은 이름의 빈이 중복 등록됨. `model/repository`에 있는 파일만 남기고 다시 빌드해서 해결.

조회수는 필드만 추가한 상태이며 증가 기능은 아직 구현하지 않음.

관련 파일: [Post.java](src/main/java/com/example/demo/model/domain/Post.java), [PostRepository.java](src/main/java/com/example/demo/model/repository/PostRepository.java), [PostService.java](src/main/java/com/example/demo/model/service/PostService.java), [BoardController.java](src/main/java/com/example/demo/controller/BoardController.java)

<a id="week10"></a>

## 10주차 - 페이징, 검색, 댓글

### 페이징과 검색

- `PageRequest`로 최신 글부터 10개씩 조회
- `Page`의 전체 글 수와 페이지 수를 화면에 표시
- 주소의 `page`는 0부터, 화면의 페이지 번호는 1부터 시작
- 음수 페이지는 첫 페이지로 처리
- JPQL로 제목 또는 내용에 검색어가 포함된 글 조회
- 검색 결과에서 페이지를 바꿔도 검색어 유지
- `/board/my`에서 내가 쓴 글만 조회

### 댓글

- `Comment` 엔티티와 `comments` 테이블 추가
- 댓글을 글, 작성자와 `@ManyToOne`으로 연결
- 로그인한 회원만 등록 가능, 작성자는 서버에서 지정
- 빈 댓글과 500자를 넘는 댓글은 등록 차단
- 오래된 댓글부터 표시하고 목록 제목 옆에 댓글 수 표시
- 댓글 작성자 또는 관리자만 삭제 가능
- 다른 글에 속한 댓글 번호로 삭제 요청하면 404 처리
- 등록·삭제 후 상세 페이지의 댓글 위치로 리다이렉트

| 요청 | 기능 |
| --- | --- |
| `GET /board?page=0` | 첫 페이지 |
| `GET /board?keyword=스터디&page=1` | 스터디 검색 결과의 두 번째 페이지 |
| `GET /board/my` | 내가 쓴 글 |
| `POST /board/{postId}/comments` | 댓글 등록 |
| `POST /board/{postId}/comments/{id}/delete` | 댓글 삭제 |

### 연관 데이터 삭제

- 글 삭제: `CascadeType.REMOVE`로 댓글을 먼저 삭제한 뒤 글 삭제
- 회원 삭제: 작성한 글이나 댓글이 있으면 삭제를 막고 안내 문구 표시
- 양방향 연관관계의 무한 참조를 막기 위해 Lombok의 `@ToString.Exclude`, `@EqualsAndHashCode.Exclude` 사용

### 오류 정리

- 제공 HTML과 CSS를 잘못된 폴더에 넣어 기존 화면이 계속 나옴. HTML은 `templates/board`, CSS는 `static/css`로 옮겨서 해결.
- `CommentRepository`도 `model/repository` 한 곳에만 배치.

### 실습 확인

- 테스트 글 105개를 넣어 기존 글과 합쳐 106개, 11페이지 확인
- `스터디` 검색 결과 13개, 두 번째 페이지에 3개 표시
- 비로그인 상태에서 댓글 영역과 로그인 안내 확인
- 페이징, 검색, 댓글 등록·삭제 권한, 글 삭제 시 댓글 삭제, 회원 삭제 제한 테스트

관련 파일: [list.html](src/main/resources/templates/board/list.html), [detail.html](src/main/resources/templates/board/detail.html), [board.css](src/main/resources/static/css/board.css), [Comment.java](src/main/java/com/example/demo/model/domain/Comment.java), [CommentController.java](src/main/java/com/example/demo/controller/CommentController.java), [CommentService.java](src/main/java/com/example/demo/model/service/CommentService.java), [CommentRepository.java](src/main/java/com/example/demo/model/repository/CommentRepository.java)

<a id="week11"></a>

## 11주차 - 신청 바구니와 트랜잭션

### 모집글과 포스터

- 기존 게시글에 정원(`capacity`), 신청 인원(`appliedCount`), 포스터 파일명(`posterFile`) 추가
- 정원을 비우면 일반 글로 저장하고, 모집글의 정원은 1~100명으로 제한
- 목록에 `모집 0/5` 또는 `마감`을 표시하고, 상세에서 정원·신청 인원·남은 자리 확인
- `FileStore`에서 이미지 확장자와 파일 시그니처를 검사한 뒤 `uploads/`에 UUID 이름으로 저장. DB에는 파일명만 저장
- `WebConfig`에서 `/uploads/`로 포스터를 제공. 업로드 제한은 파일 한 개 5MB, 요청 전체 10MB로 설정

### 신청 바구니와 신청 확정

- 로그인한 회원의 바구니에 모집글 번호를 `LinkedHashSet`으로 저장해 중복 없이 담은 순서를 유지
- 신청 확정 시 중복 신청과 마감 여부를 확인한 뒤 `enrollments`에 내역을 저장하고 게시글의 신청 인원을 1명 늘림
- 성공하면 바구니를 비우고 확정 건수를 표시. 실패하면 바구니를 유지하고 실패 이유를 표시

| 요청 | 기능 |
| --- | --- |
| `GET /cart` | 신청 바구니와 내 신청 내역 확인 |
| `POST /cart/{postId}` | 모집글 담기 |
| `POST /cart/{postId}/remove` | 모집글 빼기 |
| `POST /cart/confirm` | 바구니에 담은 모집글 신청 확정 |

### 트랜잭션 실습

[11주차_신청테스트.sql](11주차_신청테스트.sql)로 정원 5명인 A·B와 정원 1명인 C를 준비. `student1`이 B와 C를 순서대로 담고, `student3`이 C를 먼저 신청해 마감시킨 뒤 `student1`이 모두 신청 확정을 누르는 흐름을 비교.

| 단계 | 결과 |
| --- | --- |
| PDF 37페이지 | C에서 마감 안내가 뜨고, 먼저 처리된 B는 `1/5`로 남음 |
| `@Transactional` 적용 후 | C에서 마감 안내가 뜨면 B 신청 내역과 인원 증가도 롤백되어 B는 `0/5`로 유지 |

### 오류 정리

- 새로 만든 `WebConfig.java`, `CartController.java`에 패키지 선언이 빠져 Spring이 설정과 컨트롤러를 찾지 못함. 각 파일의 패키지를 선언해 해결.
- 신청 확정 버튼이 400 오류를 반환함. 이전 서버가 `/cart/confirm`의 `confirm`을 모집글 번호로 해석하고 있었고, 서버를 재시작해 신청 확정 경로를 반영함.

관련 파일: [FileStore.java](src/main/java/com/example/demo/model/service/FileStore.java), [CartController.java](src/main/java/com/example/demo/controller/CartController.java), [Enrollment.java](src/main/java/com/example/demo/model/domain/Enrollment.java), [EnrollmentService.java](src/main/java/com/example/demo/model/service/EnrollmentService.java), [cart.html](src/main/resources/templates/cart.html)

## 폴더 구성

- `controller`: 화면 요청과 API 처리
- `model/domain`: JPA 엔티티
- `model/dto`: 요청·응답에 사용하는 DTO
- `model/repository`: DB 조회와 저장
- `model/service`: 기능 처리와 입력값·권한 검사
- `resources/templates`: Thymeleaf 화면
- `resources/static`: CSS, JS, 이미지, 폰트

## 실행 방법

MySQL을 실행하고 `spring` 데이터베이스 생성.

```sql
CREATE DATABASE spring;
```

[설정 예시](src/main/resources/application-secret.properties.example)를 참고해 `src/main/resources/application-secret.properties` 작성.

```properties
spring.datasource.username=root
spring.datasource.password=본인의_MySQL_비밀번호
app.security.remember-me-key=임의의_긴_비밀키
```

비밀번호가 들어 있는 설정 파일은 Git에서 제외.

`pom.xml`이 있는 폴더에서 실행.

```sh
./mvnw spring-boot:run
```

Windows에서는 `mvnw.cmd spring-boot:run` 사용. VS Code에서는 DB 실행 후 `DemoApplication` 실행.

- 메인: <http://localhost:8080>
- 게시판: <http://localhost:8080/board>
- 로그인: <http://localhost:8080/login>
- 회원 관리: <http://localhost:8080/admin/members>

실습 계정은 `student1`(USER), `admin`(ADMIN). 다른 DB에서 실행할 때는 회원가입과 관리자 권한 설정이 필요함. 10주차 테스트 글도 제공 SQL로 별도 입력.

### 실행할 때 확인할 것

- DB 연결 오류: Docker, MySQL 실행 여부와 3306 포트 확인
- 계정 오류: `application-secret.properties`의 아이디와 비밀번호 확인
- 8080 포트 중복: 기존 서버를 종료한 뒤 실행
- 권한 변경이 적용되지 않을 때: 로그아웃 후 재로그인

## 테스트

```sh
./mvnw test
```

테스트는 H2 메모리 DB 사용. 실제 실행 DB는 MySQL 사용.

- `DemoApplicationTests`: 서버 설정과 컨텍스트
- `MemberServiceTests`: 가입, 암호화, 중복 검사, 비밀번호 확인
- `LoginFlowTests`: 로그인, 로그아웃, CSRF, 로그인 상태 유지
- `AuthorizationTests`: 권한별 접근, 관리자 본인 보호
- `BoardFlowTests`: 페이징, 검색, 댓글, 삭제 권한과 연관 데이터 삭제

10주차 기준 `./mvnw -o verify` 테스트 19개 통과, JAR 빌드 완료.
