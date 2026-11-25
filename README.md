# SPRING_BOOT_20220672

자바웹프로그래밍(2) 수업 실습 노트

학번 20220672 · Java 25 · Spring Boot 4.1.1 · MySQL 8

[7주차](#week7) · [9주차](#week9) · [10주차](#week10) · [11주차](#week11) · [12주차](#week12) · [실행 방법](#실행-방법)

## 1~2주차 - 개발 환경 설정

- JDK 설치하고 Spring Boot 프로젝트 만들기.
- `DemoApplication`을 실행해서 8080 포트로 접속. `/hello`에서 컨트롤러가 `Model`에 넣은 값을 Thymeleaf로 출력했다.

## 3주차 - 포트폴리오 화면

- TemplateMo의 First Portfolio 템플릿을 가져와 소개, 기술, 프로젝트, 연락처 메뉴를 만들었다.
- CSS, JS, 이미지, 폰트는 `static`에 넣고 [index.html](src/main/resources/templates/index.html)에서 Thymeleaf로 경로 연결.
- 웹, AI, 보안, 게임 상세 페이지 추가.

## 4주차 - MySQL 연동

- Spring Data JPA와 MySQL 드라이버를 추가하고 `localhost:3306/spring`에 연결.
- `TestDB` 엔티티를 만들고 Controller → Service → Repository 순서로 데이터를 가져와 `/testdb`에 표시했다.
- 연습문제로 나이(`age`)와 성별(`gender`) 컬럼을 추가하고 MySQL 확장에서 데이터 입력.

```sql
INSERT INTO testdb (id, name) VALUES (1, '홍길동');
```

## 5주차 - 회원가입과 로그인

- 회원가입, 로그인, 로그아웃을 만들고 비밀번호는 BCrypt로 암호화해서 저장.
- 가입할 때 아이디 중복과 비밀번호 확인값 검사. 가입에 실패하면 입력한 아이디와 이름은 화면에 남겨 뒀다.
- 로그인 여부에 따라 메뉴를 다르게 표시. 로그아웃은 CSRF 토큰을 넣어 POST로 요청.
- `remember-me` 쿠키로 로그인 상태를 7일간 유지.

## 6주차 - 권한과 회원 관리

- `/mypage`에서 로그인한 회원 정보 조회. 관리자 화면에서는 회원 목록 조회, 권한 변경, 삭제 가능.
- 관리자가 자기 권한을 바꾸거나 자기 계정을 삭제하는 경우는 막았다.

| 권한 | `/mypage` | `/testdb` | `/admin/**` |
| --- | --- | --- | --- |
| USER | 가능 | 403 | 403 |
| MANAGER | 가능 | 가능 | 403 |
| ADMIN | 가능 | 가능 | 가능 |

관리자 계정은 먼저 회원가입하고 MySQL에서 권한을 바꾼다. 이미 로그인한 상태면 로그아웃하고 다시 로그인.

```sql
UPDATE member SET role = 'ADMIN' WHERE username = 'admin';
```

<a id="week7"></a>

## 7주차 - REST API와 JSON

- `MemberResponse`로 회원 정보를 JSON으로 보내고 비밀번호는 응답에서 뺐다.
- 관리자 페이지 `/admin/members-api`에서는 `fetch`로 목록, 등록, 권한 변경, 삭제 요청.
- `?keyword=student`로 아이디나 이름을 검색. 잘못된 입력은 `@RestControllerAdvice`에서 400으로 처리했다.

| 요청 | 하는 일 |
| --- | --- |
| `GET /api/hello` | 메시지와 시간 보기 |
| `GET /api/me` | 로그인한 내 정보 보기 |
| `GET /api/admin/members` | 회원 목록과 검색 |
| `GET /api/admin/members/{id}` | 회원 한 명 조회 |
| `POST /api/admin/members` | 회원 등록 |
| `PATCH /api/admin/members/{id}/role` | 권한 변경 |
| `DELETE /api/admin/members/{id}` | 회원 삭제 |

Postman에서는 Authorization을 Basic Auth로 설정하고 가입한 계정으로 요청했다. 로그인 정보 없이 `/api/me`를 요청하면 401, 일반 회원으로 관리자 API를 요청하면 403. 권한을 바꿀 때는 요청 본문에 `{"role":"ADMIN"}`을 넣는다.

<a id="week9"></a>

## 9주차 - 게시판 CRUD

- `Post`에 제목, 내용, 작성자, 작성·수정 시간, 조회수 필드를 넣고 작성자는 `@ManyToOne`으로 연결.
- 게시판 목록, 상세, 글쓰기, 수정, 삭제를 만들었다. 작성자는 로그인 정보에서 가져온다.
- 제목과 내용이 비어 있거나 제목이 100자를 넘으면 저장을 막고, 없는 글을 열면 404로 처리.

| 요청 | 권한 |
| --- | --- |
| `GET /board`, `GET /board/{id}` | 누구나 |
| `GET /board/write`, `POST /board/write` | 로그인한 회원 |
| `GET /board/{id}/edit`, `POST /board/{id}/edit` | 작성자 |
| `POST /board/{id}/delete` | 작성자 또는 관리자 |

`PostRepository`를 두 패키지에 넣었더니 빈 이름이 겹쳤다. `model/repository` 쪽 파일만 남겨서 해결했다.

<a id="week10"></a>

## 10주차 - 페이징, 검색, 댓글

- 최신 글부터 10개씩 가져오도록 `PageRequest`를 적용. 주소의 `page=0`이 화면의 1페이지다.
- 제목이나 내용으로 검색하고, 페이지를 넘겨도 검색어가 남도록 했다. `/board/my`에서는 내가 쓴 글만 보인다.
- 댓글은 로그인한 회원만 등록할 수 있고, 목록의 글 제목 옆에 댓글 수가 나온다. 삭제는 댓글 작성자와 관리자만 가능.
- 글을 삭제하면 연결된 댓글도 삭제. 작성한 글이나 댓글이 있는 회원은 삭제를 막았다.

제공 파일을 넣고도 화면이 그대로였는데 HTML은 `templates/board`, CSS는 `static/css`에 넣어야 했다.

<a id="week11"></a>

## 11주차 - 신청 바구니와 트랜잭션

- 글을 쓸 때 정원을 입력하면 모집글이 된다. 상세에서 신청 인원과 남은 자리를 볼 수 있다.
- 포스터는 `uploads/`에 저장하고 DB에는 파일명만 넣었다. 이미지 주소는 `/uploads/파일명`.
- 모집글을 바구니에 담고 한꺼번에 신청할 수 있게 했다. 중복 신청이거나 정원이 찬 글이 있으면 안내가 뜬다.
- 내 신청 내역에 취소 버튼을 넣었다. 본인만 취소할 수 있고, 취소하면 신청 인원이 한 명 줄어든다.
- 신청자가 있는 글과 신청 내역이 있는 회원은 삭제를 막았다. 신청자가 없는 글을 지우면 포스터도 같이 삭제된다.

[11주차_신청테스트.sql](11주차_신청테스트.sql)로 모집글을 넣고 B와 C를 바구니에 담았다. 다른 계정에서 정원 1명인 C를 먼저 마감시킨 뒤 두 글을 한 번에 신청했다. 처음에는 먼저 처리된 B에 신청이 남았다. `@Transactional`을 적용하니 C에서 실패할 때 B도 함께 취소되어 `0/5`로 남았다.

신청 확정 버튼에서 400이 뜬 적이 있다. 이전 서버가 `/cart/confirm`의 `confirm`을 모집글 번호로 읽고 있어서 서버를 다시 실행했다.

<a id="week12"></a>

## 12주차 - WebSocket 실시간 알림

- 게시판과 신청 바구니에서 `/ws`에 연결. 신청이 들어오면 `/topic/seats`로 바뀐 인원을 보내서 새로고침 없이 남은 자리와 마감 표시를 바꿨다.
- B와 C를 같이 신청할 때 C가 이미 마감됐으면 DB에서는 B의 신청도 취소된다. 이때 메시지를 먼저 보내면 화면에만 B의 인원이 늘어난 채로 남는다. 그래서 `@TransactionalEventListener`로 DB 커밋이 끝난 뒤에 메시지를 보냈다.
- 신청하거나 취소했을 때 모집글 작성자에게 알림을 보낸다. 정원 1명인 글에서 신청을 취소하면 마감 표시가 모집 중으로 바뀌고 신청 버튼이 다시 나온다.
- 다른 사람이 댓글을 달면 글쓴이에게 알림이 간다. 자기 글에 단 댓글은 알림을 보내지 않는다.

게시판에 `실시간 연결 안 됨`이 떴는데, VS Code에서 Maven 의존성을 다시 불러오고 서버를 재시작하니 연결됐다.

알림 코드는 [RealtimeNotifier.java](src/main/java/com/example/demo/model/service/RealtimeNotifier.java), 화면 쪽은 [realtime.js](src/main/resources/static/js/realtime.js)에 있다.

## 실행 방법

MySQL을 켜고 `spring` 데이터베이스를 만든다.

```sql
CREATE DATABASE spring;
```

[설정 예시](src/main/resources/application-secret.properties.example)를 보고 `src/main/resources/application-secret.properties`를 만든다. MySQL 계정과 비밀번호, 로그인 유지에 사용할 키를 넣으면 된다.

```properties
spring.datasource.username=root
spring.datasource.password=본인의_MySQL_비밀번호
app.security.remember-me-key=임의의_긴_비밀키
```

`pom.xml`이 있는 폴더에서 실행.

```sh
./mvnw spring-boot:run
```

Windows에서는 `mvnw.cmd spring-boot:run`, VS Code에서는 `DemoApplication`을 실행한다. DB 연결이 안 되면 Docker에서 MySQL이 켜져 있는지와 3306 포트를 확인한다.

[메인](http://localhost:8080/) · [게시판](http://localhost:8080/board) · [로그인](http://localhost:8080/login) · [회원 관리](http://localhost:8080/admin/members)
