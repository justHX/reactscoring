package ru.fitkb.nkarin.scoringreact.dao;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.EntityWriteResult;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.fitkb.nkarin.scoringreact.dao.entity.PersonData;
import ru.fitkb.nkarin.scoringreact.dao.entity.Reports;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportsDao {

    private final ReactiveCassandraTemplate reactiveCassandraTemplate;

    private static final ConsistencyLevel CONSISTENCY_LEVEL_WRITE = ConsistencyLevel.ONE;
    private static final ConsistencyLevel CONSISTENCY_LEVEL_READ = ConsistencyLevel.ONE;

    public Mono<Reports> save(Reports reports) {
        log.debug("save: start: reports = {}", reports);

        InsertOptions insertOptions =
                InsertOptions.builder().consistencyLevel(CONSISTENCY_LEVEL_WRITE).build();

        return reactiveCassandraTemplate.insert(reports, insertOptions).map(EntityWriteResult::getEntity);
    }

    public Mono<Reports> get(String serialNumber) {
        log.debug("get: start with serial and number = {}", serialNumber);

        Select.Where select = QueryBuilder.select().from("reports")
                                          .where(QueryBuilder.eq("serialandnumber", serialNumber));
        select.setConsistencyLevel(CONSISTENCY_LEVEL_READ);

        return reactiveCassandraTemplate.selectOne(select, Reports.class);
    }
}
