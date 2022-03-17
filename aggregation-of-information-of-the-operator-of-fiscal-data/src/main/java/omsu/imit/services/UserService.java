package omsu.imit.services;

import com.google.gson.JsonObject;
import omsu.imit.dto.request.OfdTokenRequest;
import omsu.imit.models.User;
import omsu.imit.repo.UserCrudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserCrudRepository userCrudRepository;

    @Autowired
    private OfdService ofdService;

    public ResponseEntity<?> addUser(OfdTokenRequest ofdTokenRequest) {
        userCrudRepository.addUser(ofdTokenRequest.getLogin(), ofdTokenRequest.getPassword());
        if(login()==null){
            userCrudRepository.deleteAll();
            return new ResponseEntity<>("UserService login : Ошибка при получении токена от OFD.ru, неправильные логин/пароль", HttpStatus.BAD_REQUEST);
        }
        else{
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

    public String login() {
        User user = getUser();
        if(user.getExpirationDate()!=null && user.getExpirationDate().isAfter(LocalDateTime.now())){
            return user.getToken();
        }
        JsonObject jsonObject = ofdService.loginPostPlainJSON();
        if (jsonObject == null) {
            LOGGER.error("UserService login : Ошибка при получении токена от OFD.ru, неправильные логин/пароль");
            return null;
        }
        LOGGER.info("Токен был создан/обновлён успешно!");
        user.setToken(jsonObject.get("AuthToken").getAsString());
        String time = jsonObject.get("ExpirationDateUtc").getAsString();
        user.setExpirationDate(LocalDateTime.parse(time));
        updateUser(user);
        return user.getToken();
    }
}
