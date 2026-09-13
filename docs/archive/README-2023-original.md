## 프로젝트 소개
![image](https://github.com/Step3-kakao-tech-campus/Team10_BE/assets/78211281/19affea2-2da0-4a67-bead-985f4f831224)

뽀득뽀득은 `사용자와 세차장 관리자 모두를 위한 셀프세차 예약 플랫폼`입니다.

이 서비스는 예약 관리를 자동화함으로써 사용자와 세차장 관리자 모두에게 편의성을 제공합니다. <br/>
사용자들은 원하는 시간에 세차장을 예약할 수 있고, 관리자는 자신의 세차장을 손쉽게 홍보하고 예약을 관리하여 매출을 증대시킬 수 있습니다.


## 서비스 주요 기능

- **예약 시스템**
  - 예약 생성, 조회, 수정, 삭제 기능 구현
  - 등록을 하면 자동으로 관리할 수 있는 방법을 예약시스템의 도입으로 해소
  - 세차장을 영업시간과 가격을 등록하고 세차장에서 가용가능한 베이를 지정한 하면 세차장 예약관리의 자동화가 가능
- **세차장 검색 시스템**
  - 위치기반 검색, 키워드 기반 검색 기능 구현
  - 세차장을 찾을때 한눈에 들어오지 않던 키포인트들 -> 키워드 검색을 통해 각 세차장마다
    어떤 키포인트들이 있고, 이를 검색할 수 있음
  - 위치기반한 세차장 찾기 시스템 도입으로, 세차장과 나와의 거리를 파악할 수 있도록 작성
- **리뷰 시스템**
  - 각 세차 예약 건수마다 리뷰를 작성할 수 있음. 해당 리뷰에 대한 키워드로 해당 세차장에 대한 사용자 입장의 평가가 어떠한지 한눈에 파악 가능
  - 별점 시스템을 통해 해당 세차장의 평균 평점이 어떤지 확인할 수 있음
- **세차장 관리 시스템**
  - 세차장을 등록을 손쉽게 하고, 보유하고 있는 모든 세차장들의 매출을 한눈에 확인할 수 있고, 개별 세차장의 예약 현황과 매출 관리를 쉽게 확인할 수 있도록 만듦
- **결제하기**
  - 카카오페이 api를 사용하여 mock 결제 절차를 구현
  - 결제 금액에 관련한 신뢰성있는 로직을 구현하여 결제 신뢰성 확보

## 프로젝트 문서 링크 모음

### 개발 문서

| 문서 종류          | 링크                                                                               |
| ------------------ | ---------------------------------------------------------------------------------- |
| API 문서           | [API 문서](https://succinct-washer-65e.notion.site/API-959f06119f564b16b9ec8f4333ad48de)       |
| ERD                | [ERD](https://succinct-washer-65e.notion.site/ERD-7afc84fb28d644a5b521968761612af9)            |
| 에러코드 정의서    | [에러코드 정의서](https://succinct-washer-65e.notion.site/4bdb3941f6b540dca7267cf2baf891a0)    |
| 코딩 컨벤션        | [코딩 컨벤션](https://succinct-washer-65e.notion.site/e1e6df41dc9041a5b82d88463edc3be2)        |
| Git 작업 단위      | [Git 작업 단위](https://succinct-washer-65e.notion.site/git-79d5288c3ef849d9a70134ab576e1f93)  |
| 커밋 컨벤션        | [커밋 컨벤션](https://succinct-washer-65e.notion.site/9a3a1822e6e3458cb5a21f8f291d04f3)        |
| 테스트 시나리오    | [테스트 시나리오](https://succinct-washer-65e.notion.site/2255c521c2ca4930bd8fefe148fe748b)    |
| 테스트 결과 보고서 | [테스트 결과 보고서](https://succinct-washer-65e.notion.site/155567f5974949b4b53357315c2cacc9) |

### 기획 문서

| 문서 종류     | 링크                                                                                                                                                                                                                           |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 기획 자료     | [기획 자료](https://succinct-washer-65e.notion.site/as_is_to_be-6946c5b3cba54ae892531bae4151d3d7)                                                                                                                                          |
| 기획 발표자료 | [기획 발표자료](https://www.notion.so/Weekly-ee58410d9e9f4bed871e4e4aaf7cb214?pvs=4#e326c2de2b274962bcfa04df830d7fbd)                                                                                                          |
| 와이어 프레임 | [와이어 프레임](https://www.figma.com/file/raidVFqnBM3KgJY4KFCoB1/%EB%BD%80%EB%93%9D%EB%BD%80%EB%93%9D-%EC%99%80%EC%9D%B4%EC%96%B4%ED%94%84%EB%A0%88%EC%9E%84?type=design&node-id=478%3A3616&mode=design&t=tW0gdxACISuQbhdX-1) |

### 배포 주소

| 서비스          | 링크                                                            |
| --------------- | --------------------------------------------------------------- |
| 뽀득뽀득 사용자 | [사용자 페이지](https://k923062c3c512a.user-app.krampoline.com) |
| 뽀득뽀득 관리자 | [관리자 페이지](https://kae2e326be433a.user-app.krampoline.com) |

## API문서

| 권한                     | 기능              | 설명                          |
| ------------------------ | ----------------- | ----------------------------- |
| 공통                     |                   |                               |
|                          | 🚗 세차장 조회    | 공개 세차장 정보 조회         |
|                          | 📝 리뷰 조회      | 공개 리뷰 조회                |
|                          | 🔍 검색           | 세차장, 리뷰 검색             |
|                          | 📖 예약 정보 조회 | 공개 예약 정보 조회           |
| 일반 회원, 세차장 관리자 |                   |                               |
|                          | 📅 예약 관리      | 예약 생성, 수정, 취소         |
|                          | 💬 리뷰 관리      | 리뷰 작성, 수정, 삭제         |
|                          | 🚗 세차장 조회    | 세차장 상세 정보 조회         |
|                          | 📈 통계           | 사용자 통계 조회              |
| 세차장 관리자            |                   |                               |
|                          | 🚗 세차장 관리    | 세차장 정보 등록, 수정, 삭제  |
|                          | 📅 예약 관리      | 예약 승인, 거절               |
|                          | 💬 리뷰 응답      | 리뷰에 대한 응답 작성         |
|                          | 📈 통계           | 세차장 예약 및 매출 통계 조회 |

## 예약 신뢰성

- 사용자가 요청한 예약이 운영 시간을 준수하고 중복 예약이 방지되도록 예약 검증을 철저히 하였습니다.
  - **`운영 시간` 내 예약 검증** : 운영시간 내에 예약이 이루어지는지 확인합니다.
    - **24시간 영업** 및 **새벽 영업**(11:00~2:00)을 지원합니다.

    ```java
    if (!((opStartTime.equals(LocalTime.MIDNIGHT) && opEndTime.equals(LocalTime.MIDNIGHT)) ||
          (opStartTime.isBefore(requestStartTimePart) || opStartTime.equals(requestStartTimePart)) &&
          (opEndTime.isAfter(requestEndTimePart) || opEndTime.equals(requestEndTimePart)))) {
        throw new BadRequestError(
                BadRequestError.ErrorCode.VALIDATION_FAILED,
                Collections.singletonMap("operatingHours", "Reservation time is out of operating hours")
        );
    }
    ```

  - **`중복 예약` 검증** : 다른 회원의 예약 시간과 겹치지 않는지 확인합니다.
    - 새로운 예약의 종료 시간이 `기존 예약의 시작 시간 이전`이고, 새로운 예약의 시작 시간이 `기존 예약의 종료 시간 이후`인 경우 정상 예약됩니다.

    ```java
    boolean isOverlapping = reservationList.stream()
        .anyMatch(existingReservation -> {
            LocalDateTime existingStartTime = existingReservation.getStartTime();
            LocalDateTime existingEndTime = existingReservation.getEndTime();
    
            // 새 예약이 기존 예약과 중복되는지 확인합니다.
            // 중복은 새 예약이 기존 예약의 시간과 겹칠 때 발생합니다.
            return !(endTime.isEqual(existingStartTime) || startTime.isEqual(existingEndTime)) &&
                   !(endTime.isBefore(existingStartTime) || startTime.isAfter(existingEndTime));
        });
        if (isOverlapping) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.DUPLICATE_RESOURCE,
                    Collections.singletonMap("Reservation time", "Reservation time overlaps with an existing reservation.")
            );
        }
    ```

  - **`날짜가 넘어가는 예약`**(23:00~01:00) 지원 :  종료 시간에 하루를 더함으로써 정상적으로 예약을 처리합니다.

      ```java
      LocalDateTime adjustedEndTime = endTime;
      if (endTime.toLocalTime().isBefore(startTime.toLocalTime())) {
          adjustedEndTime = endTime.plusDays(1);
      }
      long minutesBetween = Duration.between(startTime, adjustedEndTime).toMinutes();
      ```

  - **`예약 시간` 배수 검증** : 예약 시간이 30분의 배수인지 확인합니다.
  - `최소 예약` **시간 검증** **:** 예약 시간이 최소 30분 이상인지 검사합니다.
- 예약 권한별 가이드라인
  - **예약 조회**
    - 사용자는 자신의 예약 내역만 조회할 수 있습니다.
    - 사장님은 소유한 매장의 모든 예약을 조회할 수 있습니다.
  - **예약 수정**
    - 사용자는 자신의 예약만 수정할 수 있습니다.
  - **예약 취소**
    - 사용자는 자신의 예약만 취소할 수 있습니다.
  - **예약 차단**
    - 사장님은 소유한 매장의 베이(예약공간)에 대해, 예약을 일시적으로 받지 못하도록 예약 차단 및 해제가 가능합니다.
  - **예약 통계**
    - 사장님은 소유한 매장에 대해 **총 매출 및 판매 수익**, **예약 현황**을 관리자 페이지에서 손쉽게 파악할 수 있습니다.

## **결제 신뢰성**

### **카카오페이 기반 결제 시스템**

- `카카오페이 결제 API`를 사용하여 결제 준비 및 결제 승인을 처리합니다.

### **금액 검증 메커니즘**

- **프론트엔드 금액 검증**: 프론트엔드에서 요청한 결제 금액(**`total_amount`**)과 서버 측에서 계산한 금액을 비교하여 결제 오류를 미연에 방지합니다.
- 프론트엔드와 서버 측의 계산 금액이 다를 경우, **`BadRequestError`**(`400`) 예외 (**`1001 VALIDATION_FAILED`** 에러 코드)를 발생시킵니다.
```java
// 금액 계산
int perPrice = carwash.getPrice();
int minutesDifference = (int) ChronoUnit.MINUTES.between(startTime, endTime);
int blocksOf30Minutes = minutesDifference / 30;
int price = perPrice * blocksOf30Minutes;
int totalAmount = price;

// 검증
if (totalAmount != requestDto.getTotal_amount())
    throw new BadRequestError(
            BadRequestError.ErrorCode.WRONG_REQUEST_TRANSMISSION,
            Collections.singletonMap("pay", "Invalid pay amount")
    );
```
## 검증

- **AOP를 사용한 유효성 검증**
  - **`GlobalValidationHandler`에서** `POST`와 `PUT` 요청에 대한 공통적인 유효성 검증 로직을 정의했습니다.
  - 유효성 검증 오류가 있을 경우 **`BadRequestError`**(`400`) 예외를 발생시켰습니다.
- **DTO 내부의 `@Valid` 어노테이션을 사용한 검증 : `@NotBlank`**, **`@Size`**, **`@NotNull`** 등의 어노테이션을 사용하여 각 필드의 유효성 규칙을 생성하고 검증했습니다.

```java
@Getter
@Setter
public static class FileUpdateDTO {
    @NotBlank(message = "File URL is required")
    private String url;

    @PastOrPresent(message = "Uploaded time must be in the past or present")
    private LocalDateTime uploadedAt;
}
```

## 예외 처리

### **명확한 예외 처리 및 상태코드 할당**

- `HTTP 상태 코드`에 따라 예외 클래스를 정의하여 예외를 분류할 수 있습니다.

![Untitled](readme_images/Untitled.png)

- 상태 코드 내에서 `사용자 정의 에러 코드`를 정의하여 명확하게 오류의 세부 사항을 쉽게 파악할 수 있습니다.

```java
public enum ErrorCode implements ApiException.ErrorCode {
    VALIDATION_FAILED(1001, "Request Validation Failed"),
    WRONG_REQUEST_TRANSMISSION(1002, "Wrong Request Transmission"),
    MISSING_PART(1003, "Missing essential part"),
    DUPLICATE_RESOURCE(1004, "Duplicate Resource");
    ...
}
```

- 예외 발생시 `status`(상태 코드), `code`(사용자 정의 에러코드), `message`(에러 메세지) 형식을 지켜 일관된 응답이 전송되도록 만들었습니다.
<img width="444" alt="image" src="https://github.com/Step3-kakao-tech-campus/Team10_BE/assets/78211281/abe61e0f-9a86-4529-852e-e5edbe2cf1ed">

- **`GlobalExceptionHandler`** 에서 다양한 유형의 예외를 잡고 적절한 HTTP 응답을 반환하도록 했습니다.
- `DefaultErrorAttributes` 클래스를 확장한 **`CustomErrorAttributes` 를** 구현하여 핸들러가 잡지 못한 모든 예외에 대해서도 일관된 응답 형식을 보장했습니다.

```java
@Override
public Map<String, Object> getErrorAttributes(
        WebRequest webRequest, ErrorAttributeOptions options) {
    Map<String, Object> defaultErrorAttributes = super.getErrorAttributes(webRequest, options);

    Map<String, Object> customErrorAttributes = new LinkedHashMap<>();
    boolean errorOccurred = !defaultErrorAttributes.get("status").equals(200);
    int status = (int) defaultErrorAttributes.get("status");

    if (errorOccurred) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();

        Object defaultMessage = defaultErrorAttributes.get("message");
        String customMessage = (status == 404) ? "Not Found" : String.valueOf(defaultMessage);

        errorDetails.put("status", String.valueOf(status));
        errorDetails.put("code", NotFoundError.ErrorCode.RESOURCE_NOT_FOUND.getCode());
       ...
    return customErrorAttributes;
}
```
- 새로운 예외 유형을 추가할 수 있도록 확장 가능하게 설계했습니다.

## 보안

### 파일 신뢰성

- `파일 확장자` 검증 : 지정 파일 형식만 업로드를 허용하여 보안 및 데이터 무결성을 강화합니다.

```java
static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");
```

- `고유 파일명`: UUID를 통해 고유한 파일 이름을 생성하여 원본 파일명이 노출되지 않고 파일명 충돌을 방지합니다.

```java
static final List<String uniqueFilename = UUID.randomUUID().toString() + extension;
        String keyName = "uploads/" + uniqueFilename;> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");
```

### CORS 설정 환경별 분리

- 와일드 카드(`*`)를 사용하지 않고 `특정 출처`만 허용하도록 설정했습니다.
- 개발 환경과 운영 환경에 따라 CORS 설정을 구분했습니다.
  - `개발 환경`: 로컬 url로 지정된 특정 출처만을 허용했습니다.
  - `운영 환경`: 특정 패턴을 설정하여 허용되는 출처를 엄격히 제한했습니다.

```java
@Bean
    @Profile("!prod")
    public CorsConfigurationSource devCorsConfigurationSource() {
        // 개발 환경용 CORS 설정
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedOrigin(frontlocalurl);
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    @Profile("prod")
    public CorsConfigurationSource prodCorsConfigurationSource() {
        // 운영 환경용 CORS 설정
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        // USER, OWNER 배포 주소 (React)
        configuration.addAllowedOriginPattern(prodfrontuserurl);
        configuration.addAllowedOriginPattern(prodfrontownerurl);
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
```

### 환경 변수 설정 및 **Kubernetes Secret 사용**

![Untitled](readme_images/Untitled%201.png)

- 보안을 강화하기위해 **`@Value` 을 통해** 구성값을 소스 코드 내에 하드코딩하지 않고 외부 구성 파일과 Kubernetes의 Secret을 활용하여 관리합니다.
- 변경 가능성이 있는 key들을 환경 변수로 관리하여 재사용성을 높였습니다.

## **권한 중심의 사용자 및 관리자 경험 최적화**

- `역할 기반` 권한관리 : Role에 따른 권한 분리(Spring Security)

```java
http.authorizeRequests(authorize -> authorize
                .antMatchers("/api/open/**").permitAll()
                .antMatchers("/api/user/**").access("hasAnyRole('USER', 'OWNER')")
                .antMatchers("/api/owner/**").access("hasRole('OWNER')")
                .anyRequest().authenticated());

```

- `사용자 인증 및 JWT 토큰 처리` **:** JWT 토큰을 검증하고 권한 검사를 수행합니다.
    
    ```java
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String jwt = request.getHeader(JWTProvider.HEADER);
    
        try {
            if (jwt != null && !isNonProtectedUrl(request)) {
                DecodedJWT decodedJWT = JWTProvider.verify(jwt);
                Long id = decodedJWT.getClaim("id").asLong();
                String role = decodedJWT.getClaim("role").asString();
                ..
        chain.doFilter(request, response);
    }
    
    private boolean isNonProtectedUrl(HttpServletRequest request) {
        AntPathRequestMatcher openMatcher = new AntPathRequestMatcher("/api/open/**");
        return openMatcher.matches(request);
    }
    ```
    

- **컨트롤러 분리 :** **공통(유저, 사장님) / 유저 / 사장님 / 둘러보기** 권한으로 분류했습니다.


![Untitled](readme_images/Untitled%202.png)

## 리팩토링

- **재사용성** : 반복되어 사용되는 메서드들을 추출해 utils로 분리하여 코드 재사용성을 높였습니다.

![Untitled](readme_images/Untitled%203.png)

- **코딩 컨벤션** : 코딩 컨벤션에 따라 클래스, 메서드명, 변수명등을 작성하고 띄어쓰기 및 중괄호
  스타일등을 지켜 코드 통일성을 높였습니다.

## 프로젝트 구조

```
📦src
 ┣ 📂main
 ┃ ┣ 📂java
 ┃ ┃ ┣ 📂bdbe
 ┃ ┃ ┃ ┣ 📂bdbd
 ┃ ┃ ┃ ┃ ┣ 📂_core
 ┃ ┃ ┃ ┃ ┃ ┣ 📂config
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜AWSConfig.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜RestTemplateConfig.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂exception
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜ApiException.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜BadGatewayError.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜BadRequestError.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜CustomErrorAttributes.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜ForbiddenError.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜InternalServerError.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜NotFoundError.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜UnAuthorizedError.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂handler
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜GlobalExceptionHandler.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜GlobalValidationHandler.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂security
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜CustomUserDetails.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜CustomUserDetailsService.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜JWTProvider.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜JwtAuthenticationFilter.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜SecurityConfig.java
 ┃ ┃ ┃ ┃ ┃ ┗ 📂utils
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜ApiUtils.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜DateUtils.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜FileUploadUtil.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜FilterResponseUtils.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜Haversine.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜KeywordTypeConverter.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜MemberUtils.java
 ┃ ┃ ┃ ┃ ┣ 📂controller
 ┃ ┃ ┃ ┃ ┃ ┣ 📂common
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CommonMemberController.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂open
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜OpenCarwashController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜OpenMemberController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜OpenReservationController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜OpenReviewController.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂owner
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜OwnerBayRestController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜OwnerCarwashController.java
 ┃ ┃ ┃ ┃ ┃ ┗ 📂user
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜UserPayController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜UserReservationController.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜UserReviewController.java
 ┃ ┃ ┃ ┃ ┣ 📂dto
 ┃ ┃ ┃ ┃ ┃ ┣ 📂bay
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜BayRequest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜BayResponse.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂carwash
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜CarwashRequest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CarwashResponse.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂file
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜FileRequest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜FileResponse.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂member
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂owner
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜OwnerResponse.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📂user
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜UserRequest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜UserResponse.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂pay
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜PayRequest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜PayResponse.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂reservation
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜ReservationRequest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReservationResponse.java
 ┃ ┃ ┃ ┃ ┃ ┗ 📂review
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜ReviewRequest.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReviewResponse.java
 ┃ ┃ ┃ ┃ ┣ 📂model
 ┃ ┃ ┃ ┃ ┃ ┣ 📂bay
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Bay.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂carwash
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Carwash.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂file
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜File.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂keyword
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂carwashKeyword
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CarwashKeyword.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂reviewKeyword
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReviewKeyword.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Keyword.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂location
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Location.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂member
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Member.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂optime
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Optime.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂reservation
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Reservation.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂review
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜Review.java
 ┃ ┃ ┃ ┃ ┃ ┗ 📜Code.java
 ┃ ┃ ┃ ┃ ┣ 📂pay
 ┃ ┃ ┃ ┃ ┃ ┗ 📜.DS_Store
 ┃ ┃ ┃ ┃ ┣ 📂repository
 ┃ ┃ ┃ ┃ ┃ ┣ 📂bay
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜BayJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂carwash
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CarwashJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂file
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜FileJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂keyword
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂carwashKeyword
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CarwashKeywordJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📂reviewKeyword
 ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReviewKeywordJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜KeywordJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂location
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜LocationJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂member
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜MemberJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂optime
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜OptimeJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂reservation
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReservationJPARepository.java
 ┃ ┃ ┃ ┃ ┃ ┗ 📂review
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReviewJPARepository.java
 ┃ ┃ ┃ ┃ ┣ 📂service
 ┃ ┃ ┃ ┃ ┃ ┣ 📂bay
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜BayService.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂carwash
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜CarwashService.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂file
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜FileService.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂member
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ 📜OwnerService.java
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜UserService.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂pay
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜PayService.java
 ┃ ┃ ┃ ┃ ┃ ┣ 📂reservation
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReservationService.java
 ┃ ┃ ┃ ┃ ┃ ┗ 📂review
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ 📜ReviewService.java
 ┃ ┃ ┃ ┃ ┣ 📜.DS_Store
 ┃ ┃ ┃ ┃ ┗ 📜BdbdApplication.java
 ┃ ┃ ┃ ┗ 📜.DS_Store
 ┃ ┃ ┗ 📜.DS_Store
 ┃ ┣ 📂resources
 ┃ ┃ ┣ 📜application-local.yml
 ┃ ┃ ┣ 📜application-prod.yml
 ┃ ┃ ┗ 📜application.yml
 ┃ ┗ 📜.DS_Store
```


## 중간 피드백 반영도

### 이미지 파일 관리

- S3에 다운로드된 모든 이미지 파일은 지정된 경로에 저장
- 파일 전송의 정확성을 검증하기 위한 로직을 구현하여, 파일 전달 과정에서의 오류 최소화

### 예약 시스템 최적화

- 예약 데이터에 대한 날짜 및 상태 관리를 철저히 하여 시스템의 무결성을 보장
- 예약 시스템의 로직을 구조화하여, 관리 및 유지보수의 용이성을 강화

### 지도 검색 개선

- 사용자 위치기반 세차장 조회 서비스를 구현하기 위해 각각의 위도, 경도를 location entity로 저장하고, Haversine공식을 사용해 사용자와 세차장들간의 거리 계산
```java
public List<CarwashRequest.CarwashDistanceDTO> findNearbyCarwashesByUserLocation(CarwashRequest.UserLocationDTO userLocation) {
    List<Carwash> carwashes = carwashJPARepository.findCarwashesWithin10Kilometers(userLocation.getLatitude(), userLocation.getLongitude());

    return carwashes.stream()
            .map(carwash -> {
                double distance = Haversine.distance(userLocation.getLatitude(), userLocation.getLongitude(),
                        carwash.getLocation().getLatitude(), carwash.getLocation().getLongitude());
            ...
.sorted(Comparator.comparingDouble(CarwashRequest.CarwashDistanceDTO::getDistance))
            .collect(Collectors.toList());
}
```
- 반경 10km 내의 세차장을 검색하는 JPQL Query 작성
```java
@Query(value = "SELECT cw.* FROM carwash cw JOIN location l ON cw.l_id = l.id WHERE ST_Distance_Sphere(point(l.longitude, l.latitude), point(:longitude, :latitude)) <= 10000", nativeQuery = true)
List<Carwash> findCarwashesWithin10Kilometers(@Param("latitude") double latitude, @Param("longitude") double longitude);
```
  
### 보안 및 권한 관리

- Spring Security를 사용하여 사용자 권한 관리를 강화
- 보안 프로토콜을 업데이트하고, 접근 제어 메커니즘을 통해 시스템의 안전을 보장

### 검색 기능 개선

- 사용자가 선택한 세차장 키워드들의 교집합에 해당하는 세차장을 조회
```java
List<CarwashRequest.CarwashDistanceDTO> result = carwashesWithin10Km.stream()
                .filter(carwash -> {
                    List<Long> keywordIdsForCarwash = findKeywordIdsByCarwashId(carwash.getId());
                    return keywordIds.stream()
                            .allMatch(keywordIdsForCarwash::contains);
                }) 
```  

### 에러 코드 관리

- 시스템에서 발생할 수 있는 에러를 식별하고, 필요한 에러 모드를 정의하여 사용자에게 명확한 피드백을 제공
- 에러 핸들링 절차를 간소화하여, 개발자가 에러를 더 빠르게 식별하고 해결할 수 있도록 지원

위 각 항목은 사용자 경험을 중심으로, 안정적이고 신뢰할 수 있는 서비스 제공을 목표로 함

## ERD 설계서

![Untitled](readme_images/Untitled%204.png)


## 시퀀스 다이어그램\_사용자 관점

![Untitled](readme_images/Untitled%205.png)
