package io.jdev.miniprofiler.sql

import spock.lang.Specification

import javax.sql.DataSource

class ProfilingDataSourceSpec extends Specification {

    void "wrapper delegates close when appropriate underlying datasource implements Closeable"() {
        given: 'closeable delegate ds'
        def ds = Mock(CloseableDataSource)
        def pds = new ProfilingDataSource(ds)

        when:
        pds.close()

        then:
        1 * ds.close()
    }

    void "wrapper delegates close when appropriate underlying datasource has close method"() {
        given: 'closeable delegate ds'
        def ds = Mock(DataSourceWithCloseMethod)
        def pds = new ProfilingDataSource(ds)

        when:
        pds.close()

        then:
        1 * ds.close()
    }

    void "wrapper throws exception when underlying datasource does not have close method"() {
        given: 'closeable delegate ds'
        def ds = Mock(DataSource)
        def pds = new ProfilingDataSource(ds)

        when:
        pds.close()

        then:
        thrown(UnsupportedOperationException)
    }


    static interface CloseableDataSource extends DataSource, Closeable {}
    static interface DataSourceWithCloseMethod extends DataSource {
        void close()
    }
}
