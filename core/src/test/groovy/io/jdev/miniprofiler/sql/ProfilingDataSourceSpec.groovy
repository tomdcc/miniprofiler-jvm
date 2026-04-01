/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jdev.miniprofiler.sql

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.SQLException

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

    static interface OuterDataSource extends DataSource { }
    static interface InnerDataSource extends DataSource { }
    static interface OtherDataSource extends DataSource { }

    void "wrapper returns correct values for unwrap and isWrapperFor"() {
        given:
        def outerDs = Mock(OuterDataSource)
        def innerDs = Mock(InnerDataSource)
        _ * outerDs.isWrapperFor(InnerDataSource) >> true
        _ * outerDs.unwrap(InnerDataSource) >> innerDs
        _ * outerDs.isWrapperFor(OtherDataSource) >> false
        _ * outerDs.unwrap(OtherDataSource) >> { clz -> throw new SQLException() }
        def pds = new ProfilingDataSource(outerDs)

        expect:
        pds.isWrapperFor(ProfilingDataSource)
        pds.unwrap(ProfilingDataSource) == pds

        and:
        pds.isWrapperFor(OuterDataSource)
        pds.unwrap(OuterDataSource) == outerDs

        and:
        pds.isWrapperFor(InnerDataSource)
        pds.unwrap(InnerDataSource) == innerDs

        and:
        !pds.isWrapperFor(OtherDataSource)

        when:
        pds.unwrap(OtherDataSource)

        then:
        thrown(SQLException)


    }


    static interface CloseableDataSource extends DataSource, Closeable {}
    static interface DataSourceWithCloseMethod extends DataSource {
        void close()
    }
}
