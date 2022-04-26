package omsu.imit.interfaces;

import org.springframework.http.ResponseEntity;

public interface HttpRequest {
    ResponseEntity<String> getPostsPlainJSON(String url);
}
