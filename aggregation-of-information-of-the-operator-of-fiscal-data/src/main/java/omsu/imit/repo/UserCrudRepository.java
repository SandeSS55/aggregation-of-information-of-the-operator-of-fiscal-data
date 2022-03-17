package omsu.imit.repo;

import omsu.imit.models.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface UserCrudRepository extends CrudRepository<User,Long> {

    User findById(long id);

    @Modifying
    @Transactional
    @Query(value = "insert into user (login, password) values (:login, :password)",nativeQuery = true)
    void addUser(@Param("login")String login, @Param("password")String password);

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query(value = "update user set login=:login, password=:password, token=:token, expiration_date=:time where id=1",nativeQuery = true)
    void updateUser(@Param("login")String login, @Param("password")String password, @Param("token")String token,
                    @Param("time")LocalDateTime time);
}
