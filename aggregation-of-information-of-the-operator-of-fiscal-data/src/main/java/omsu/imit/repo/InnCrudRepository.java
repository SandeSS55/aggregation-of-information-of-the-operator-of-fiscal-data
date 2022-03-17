package omsu.imit.repo;

import omsu.imit.models.Inn;
import omsu.imit.models.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InnCrudRepository extends CrudRepository<Inn, Long> {

    @Modifying
    @Transactional
    @Query(value = "delete from inn where inn=:inn", nativeQuery = true)
    void delete(@Param("inn") long inn);

    @Modifying
    @Transactional
    @Query(value = "insert into inn (inn, name,start_load_date,user_id) values (:inn,:name,:startFrom,:user)", nativeQuery = true)
    void insertNewInn(@Param("inn") long inn, @Param("name") String name, @Param("startFrom")LocalDateTime startFrom, @Param("user") User user);

    List<Inn> findAll();

    Inn findByInn(long inn);
}
