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
 * The family of schemas for StudentState.
 */
object StudentSchema

/**
 * An StudentState schema.
 */
object StudentSchemaV1 : MappedSchema(
        schemaFamily = StudentSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentStudent::class.java)) {

    override val migrationResource: String?
        get() = "student.changelog-master";

    @Entity
    @Table(name = "student_states")
    class PersistentStudent(

            @Column(name = "fullname")
            var fullname: String,

            @Column(name = "student_id")
            var studentId: String,

            @Column(name = "school")
            var school: String,

            @Column(name = "mobile")
            var mobile: String,

            @Column(name = "email")
            var email: String,

            @Column(name = "linear_id")
            @Type(type = "uuid-char")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", "", "", UUID.randomUUID())
    }
}
