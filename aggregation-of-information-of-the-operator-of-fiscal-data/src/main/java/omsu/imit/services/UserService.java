package omsu.imit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import omsu.imit.dto.request.OfdTokenRequest;
import omsu.imit.models.User;
import omsu.imit.repo.UserCrudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserService {

    private final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final RestTemplate restTemplate;

    private final AtomicInteger tries = new AtomicInteger(1);

    @Autowired
    private UserCrudRepository userCrudRepository;

    public UserService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ResponseEntity<?> addUser(OfdTokenRequest ofdTokenRequest) {
        userCrudRepository.addUser(ofdTokenRequest.getLogin(), ofdTokenRequest.getPassword());
        if (login() == null) {
            userCrudRepository.deleteAll();
            userCrudRepository.alterTableOne();
            return new ResponseEntity<>("UserService login : Ошибка при получении токена от OFD.ru, неправильные логин/пароль", HttpStatus.BAD_REQUEST);
        } else {
            LOGGER.info("Данные для логирования в ОФД были успешно добавлены в базу.");
            return new ResponseEntity<>("Данные для логирования в ОФД были успешно добавлены в базу.", HttpStatus.OK);
        }
    }

    public User getUser() {
        return userCrudRepository.findById(1);
    }

    public void updateUser(User user) {
        userCrudRepository.updateUser(user.getLogin(), user.getPassword(), user.getToken(), user.getExpirationDate());
        LOGGER.info("Данные о пользователе были успешно обновлены.");
    }

    public ResponseEntity<?> loginPostPlainJSON(String login, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> map = new HashMap<>();
        map.put("Login", login);
        map.put("Password", password);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<String> jsonObjectResponseEntity = this.restTemplate.postForEntity
                    ("https://ofd.ru/api/Authorization/CreateAuthToken", entity, String.class);
            return new ResponseEntity<>(new Gson().fromJson(jsonObjectResponseEntity.getBody(), JsonObject.class), HttpStatus.OK);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                if (Objects.equals(ex.getMessage(), "")) {
                    LOGGER.error("Токен не был создан/обновлён по причине(ам): Неправильно введены логин/пароль");
                    return new ResponseEntity<>("Неправильно введены логин/пароль", HttpStatus.BAD_REQUEST);
                } else {
                    LOGGER.error("Токен не был создан/обновлён по причине(ам): " + ex.getMessage());
                    return new ResponseEntity<>("Токен не был создан/обновлён по причине(ам): " + ex.getMessage(), HttpStatus.BAD_REQUEST);
                }
            }
            if (ex.getStatusCode().is5xxServerError()) {
                if (tries.get() <= 3) {
                    tries.getAndIncrement();
                    loginPostPlainJSON(login,password);
                } else {
                    tries.getAndSet(1);
                    LOGGER.error("Токен не был создан/обновлён по причине(ам): OFD.ru перегружен или недоступен");
                    return new ResponseEntity<>("Токен не был создан/обновлён по причине(ам): OFD.ru перегружен или недоступен", HttpStatus.BAD_REQUEST);
                }
            }
        }
        return null;
    }

    public String login() {
        try {
            User user = getUser();
            if (user.getExpirationDate() != null && user.getExpirationDate().isAfter(LocalDateTime.now())) {
                return user.getToken();
            }
            ResponseEntity<?> responseEntity = loginPostPlainJSON(user.getLogin(),user.getPassword());
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                JsonObject jsonObject = (JsonObject) responseEntity.getBody();
                LOGGER.info("Токен был создан/обновлён успешно!");
                assert jsonObject != null;
                user.setToken(jsonObject.get("AuthToken").getAsString());
                String time = jsonObject.get("ExpirationDateUtc").getAsString();
                user.setExpirationDate(LocalDateTime.parse(time));
                updateUser(user);
                return user.getToken();
            }
        } catch (HttpClientErrorException ex) {
            LOGGER.error("UserService login : Ошибка при получении токена от OFD.ru, неправильные логин/пароль");
            return null;
        }
        return null;
    }
}
