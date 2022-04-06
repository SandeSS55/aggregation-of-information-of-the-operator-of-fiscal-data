package omsu.imit;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import omsu.imit.controllers.ShiftController;
import omsu.imit.dto.request.InnInfoRequest;
import omsu.imit.dto.request.OfdTokenRequest;
import omsu.imit.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BaseInitializer {

    @Value("${user.login}")
    private String login;
    @Value("${user.password}")
    private String password;
    @Value("${user.inn}")
    private String[] inn;
    @Value("${user.startDate}")
    private String startDate;

    @Autowired
    private ShiftController shiftController;

    @Autowired
    private UserService userService;

    private final Logger LOGGER = LoggerFactory.getLogger(BaseInitializer.class);

    @PostConstruct
    public void init() throws JsonProcessingException {
        if (userService.getUser() == null) {
            if (login != null && !login.equals("") && password != null && !password.equals("")) {
                shiftController.insertUser(new OfdTokenRequest(login, password));
                if (inn.length > 0) {
                    for (String inn : inn) {
                        try {
                            if (startDate != null && !startDate.equals("")) {
                                shiftController.insertInn(new InnInfoRequest(inn, Long.parseLong(inn), LocalDateTime.parse(startDate)));
                            } else {
                                shiftController.insertInn(new InnInfoRequest(inn, Long.parseLong(inn), null));
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Неправильно введена дата в application.properties, загрузка чеков будет осуществляться по дате первого чека в " +
                                    "фискальном накопителе");
                            shiftController.insertInn(new InnInfoRequest(inn, Long.parseLong(inn), null));
                        }
                    }
                }
            }
        }
    }
}
