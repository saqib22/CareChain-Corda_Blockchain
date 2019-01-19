package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * An IOUState schema.
 */
public class IOUSchemaV1 extends MappedSchema {
    public IOUSchemaV1() {
        super(IOUSchema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table(name = "iou_states")
    public static class PersistentIOU extends PersistentState {
        @Column(name = "hospital") private final String hospital;
        @Column(name = "patient") private final String patient;
        @Column(name = "name") private final String name;
        @Column(name = "age") private final int age;
        @Column(name = "gender") private final String gender;
        @Column(name = "height") private final int height;
        @Column(name = "weight") private final int weight;
        @Column(name = "bloodGroup") private final String bloodGroup;
        @Column(name = "diagnosis") private final String diagnosis;
        @Column(name = "medicine") private final String medicine;
        @Column(name = "linear_id") private final UUID linearId;


        public PersistentIOU(String hospital, String patient, String name, int age, String gender, int height, int weight, String bloodGroup, String diagnosis, String medicine, UUID linearId) {
            this.hospital = hospital;
            this.patient = patient;
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.height = height;
            this.weight = weight;
            this.bloodGroup = bloodGroup;
            this.diagnosis = diagnosis;
            this.medicine = medicine;
            this.linearId = linearId;
        }

        // Default constructor required by hibernate.
        public PersistentIOU() {
            this.hospital = null;
            this.patient = null;
            this.name = null;
            this.age = 0;
            this.gender = null;
            this.height = 0;
            this.weight = 0;
            this.bloodGroup = null;
            this.diagnosis = null;
            this.medicine = null;
            this.linearId = null;
        }

        public String getHospital() {
            return hospital;
        }

        public String getPatient() {
            return patient;
        }

        public String getName() {
            return name;
        }

        public int getAge(){ return age; }

        public String getGender() { return gender; }

        public int getHeight() { return height; }

        public int getWeight() { return weight; }

        public String getBloodGroup() { return bloodGroup; }

        public String getDiagnosis() { return diagnosis; }

        public String getMedicine() { return medicine; }

        public UUID getId() {
            return linearId;
        }
    }
}