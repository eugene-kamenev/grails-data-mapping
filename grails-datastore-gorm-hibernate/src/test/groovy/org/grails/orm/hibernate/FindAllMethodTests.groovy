package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class FindAllMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [FindAllTest]
    }

    @Test
    void testUsingHibernateCache() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true
        stats.clear()

        def cacheStats = stats.getSecondLevelCacheStatistics('org.hibernate.cache.StandardQueryCache')
        assertEquals 0, cacheStats.hitCount
        assertEquals 0, cacheStats.missCount
        assertEquals 0, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Angus'", [cache: true])
        assertEquals 0, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Angus'", [cache: true])
        assertEquals 1, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Angus'", [cache: true])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Angus'")
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Angus'", [cache: false])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Angus'", [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Malcolm'", [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = 'Malcolm'", [cache: true])
        assertEquals 4, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount
    }

    @Test
    void testUsingHibernateCacheWithNamedParams() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true
        stats.clear()

        def cacheStats = stats.getSecondLevelCacheStatistics('org.hibernate.cache.StandardQueryCache')
        assertEquals 0, cacheStats.hitCount
        assertEquals 0, cacheStats.missCount
        assertEquals 0, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Angus'], [cache: true])
        assertEquals 0, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Angus'], [cache: true])
        assertEquals 1, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Angus'], [cache: true])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Angus'])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Angus'], [cache: false])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Angus'], [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Malcolm'], [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount

        theClass.findAll("from FindAllTest where name = :name", [name: 'Malcolm'], [cache: true])
        assertEquals 4, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount
    }

    @Test
    void testHQLWithNamedArgs() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        theClass.newInstance(name:"fred").save(flush:true)

        assertEquals 1, theClass.findAll("from FindAllTest as t where t.name = :name", [name:'fred']).size()
        assertEquals 0, theClass.findAll("from FindAllTest as t where t.name = :name", [name:null]).size()
    }

    @Test
    void testHQLWithNamedArgsAndMultiline() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        theClass.newInstance(name:"alice").save(flush:true)

        assertEquals 1, theClass.findAll("""
                                        from FindAllTest as t 
                                        where t.name = :name
                                        """, [name:'alice']).size()
    }

    @Test
    void testStartingWithSpaceOnSingleLine() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        theClass.newInstance(name:"alice").save(flush:true)

        assertEquals 1, theClass.findAll(" from FindAllTest as t where t.name = :name", [name:'alice']).size()
    }

    @Test
    void testFindAllWithNullNamedParam() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz
        assertEquals 0, theClass.findAll(max:null).size()
    }

    @Test
    void testNoArgs() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        assertEquals 0, theClass.findAll().size()

        theClass.newInstance(name:"Foo").save(flush:true)

        assertEquals 1, theClass.findAll().size()
    }

    @Test
    void testMixedCaseHQL() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        assertEquals 0, theClass.findAll().size()

        theClass.newInstance(name:"Foo").save()
        theClass.newInstance(name:"Fred").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Stuff").save(flush:true)

        assertEquals 2, theClass.findAll("from FindAllTest as t where t.name like ? ", ['F%']).size()
        assertEquals 2, theClass.findAll("FROM FindAllTest AS t WHERE t.name LIKE ? ", ['F%']).size()
    }

    @Test
    void testWithSort() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        assertEquals 0, theClass.findAll().size()

        theClass.newInstance(name:"Foo").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Stuff").save(flush:true)

        assertEquals 3, theClass.findAll(sort:'name').size()
        assertEquals(["Bar", "Foo", "Stuff"], theClass.findAll(sort:'name').name)
    }

    @Test
    void testWithExample() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        assertEquals 0, theClass.findAll().size()

        theClass.newInstance(name:"Foo").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Bar").save()
        theClass.newInstance(name:"Stuff").save(flush:true)

        assertEquals 2, theClass.findAll(theClass.newInstance(name:"Bar"), [sort:'name']).size()
        assertEquals(["Bar", "Bar"], theClass.findAll(theClass.newInstance(name:"Bar"),[sort:'name']).name)
    }

    @Test
    void testWithExampleAndSort() {
        def theClass = ga.getDomainClass(FindAllTest.name).clazz

        assertEquals 0, theClass.findAll().size()

        theClass.newInstance(name:"Foo", index: 1).save()
        theClass.newInstance(name:"Bar", index: 2).save()
        theClass.newInstance(name:"Bar", index: 3).save()
        theClass.newInstance(name:"Stuff", index: 4).save()
        theClass.newInstance(name:"Bar", index: 5).save(flush:true)

        // Execute the query
        def results = theClass.findAll(theClass.newInstance(name: "Bar"), [max: 1])
        assertEquals 1, results.size()
        assertEquals(["Bar"], results*.name)

        // Try the sort arguments now.
        results = theClass.findAll(theClass.newInstance(name: "Bar"), [sort: "index", order: "asc"])
        assertEquals 3, results.size()
        assert [2, 3, 5] == results*.index

        // Now all the arguments together.
        results = theClass.findAll(theClass.newInstance(name: "Bar"), [sort: "index", order: "desc", offset: 1])
        assertEquals 2, results.size()
        assert [3, 2] == results*.index
    }
}

@Entity
class FindAllTest {
    Long id
    Long version
    String name
    Long index

    static constraints = {
        index(nullable: true)
    }
}

