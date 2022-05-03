package omsu.imit.repo;

import omsu.imit.models.Receipt;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ReceiptsCrudRepository extends CrudRepository<Receipt,Long> {

    @Modifying
    @Transactional
    @Query(value = "insert into receipt (receipt_number,in_correction,c_date_utc,doc_date_time,shift_number,operation_type,operator,total_summ,depth,fns_status,kkt_id) " +
            "values (:receipt_number,:in_correction,:c_date_utc,:doc_date_time,:shift_number,:operation_type,:operator,:total_summ,:depth,:fns_status,(select id from kkt k where k.fn_number=:fnNumber)))",nativeQuery = true)
    void addReceipt(@Param("receipt_number") int receiptNumber,@Param("in_correction") boolean inCorrection,
                    @Param("c_date_utc") String cDateUtc,@Param("doc_date_time") String DocDateTime,@Param("shift_number") int shiftNumber,
                    @Param("operation_type") int operationType,@Param("operator") String operator,@Param("total_summ") int totalSumm,
                    @Param("depth") int depth,@Param("fns_status") String fnsStatus,@Param("fnNumber") String fnNumber);

    @Modifying
    @Transactional
    @Query(value = "delete from receipt where kkt_id=:id",nativeQuery = true)
    void deleteAllReceiptsByKkt(@Param("id") long id);

    @Query(value = "select * from receipt where kkt_id =:id",nativeQuery = true)
    List<Receipt> findByKkt(long id);

    List<Receipt> findAll();
}
