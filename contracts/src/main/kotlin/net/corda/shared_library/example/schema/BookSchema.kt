package net.corda.shared_library.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
//4.6 changes
import org.hibernate.annotations.Type


/**
 * The family of schemas for BookState.
 */
object BookSchema

/**
 * An BookState schema.
 */
object BookSchemaV1 : MappedSchema(
        schemaFamily = BookSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBook::class.java)) {

    override val migrationResource: String?
        get() = "book.changelog-master";

    @Entity
    @Table(name = "book_states")
    class PersistentBook(
            @Column(name = "owner")
            var ownerName: String,

            @Column(name = "holder")
            var holderName: String,

            @Column(name = "title")
            var title: String,

            @Column(name = "author")
            var author: String,

            @Column(name = "ISBN")
            var ISBN: String,

            @Column(name = "isBorrowed")
            var isBorrowed: Boolean,

            @Column(name = "notification")
            @Type(type = "uuid-char")
            var notification: UUID?,

            @Column(name = "borrowDate")
            var borrowDate: String?,

            @Column(name = "linear_id")
            @Type(type = "uuid-char")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", "", "",true,null,null, UUID.randomUUID())
    }
}
