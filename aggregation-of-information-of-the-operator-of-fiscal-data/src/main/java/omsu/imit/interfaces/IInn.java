package omsu.imit.interfaces;

import omsu.imit.models.Inn;
import org.springframework.http.ResponseEntity;

public interface IInn {
    ResponseEntity<Inn> getInfoAboutCertainInn(long inn);
}
