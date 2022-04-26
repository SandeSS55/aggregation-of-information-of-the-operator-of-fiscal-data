package omsu.imit.repo;

import omsu.imit.models.Kkt;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KktCrudRepository extends CrudRepository<Kkt, Long> {


    @Modifying
    @Transactional
    @Query(value = "insert into kkt (fn_number,kkt_number,kkt_reg_number,fn_end_time,first_doc,last_doc,last_time_updated,inn_id) " +
            "values (:fn_number,:kkt_number,:kkt_reg_number,:fn_end_time,:first_doc,:last_doc,:time,(select id from inn i where i.inn = :inn))", nativeQuery = true)
    void insertKkt(@Param("fn_number") String fnNumber, @Param("kkt_number") String kktNumber,
                   @Param("kkt_reg_number") String kktRegNumber, @Param("fn_end_time") LocalDateTime fn_end_time,
                   @Param("first_doc") LocalDateTime firstDoc, @Param("last_doc") LocalDateTime lastDoc, @Param("time") LocalDateTime time,
                   @Param("inn") long inn);

    @Query(value = "select * from kkt where inn_id = :inn", nativeQuery = true)
    List<Kkt> getKkts(@Param("inn") long inn);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "update kkt u set u.fn_number = :fnNumber,u.fn_end_date=:fnEndDate,u.fiscal_address = :fiscalAddress,u.fiscal_place = :fiscalPlace,u.last_doc_on_ofd_date_time = :last_doc_on_ofd_date_time,u.last_time_updated = :last_time_updated where u.kkt_reg_number = :kkt", nativeQuery = true)
    void updateKkt(@Param("fnNumber") String fnNumber, @Param("fnEndDate") String fnEndTime,
                   @Param("fiscalAddress") String fiscalAddress, @Param("fiscalPlace") String fiscalPlace,@Param("last_doc_on_ofd_date_time") String last_doc_on_ofd_date_time,@Param("last_time_updated") String lastTimeUpdated, @Param("kkt") long kkt);

    @Modifying
    @Transactional
    @Query(value = "delete from kkt where inn_id=:id", nativeQuery = true)
    void deleteKktByInn(@Param("id") long id);

    Kkt findByKktRegNumber(String kktRegNumber);
}
