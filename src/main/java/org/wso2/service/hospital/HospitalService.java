/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.service.hospital;

import com.google.gson.Gson;
import org.wso2.service.hospital.daos.*;
import org.wso2.service.hospital.utils.HospitalUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the Microservice resource class.
 * See <a href="https://github.com/wso2/msf4j#getting-started">https://github.com/wso2/msf4j#getting-started</a>
 * for the usage of annotations.
 *
 * @since 1.0.0-SNAPSHOT
 */
@Path("/hospital/categories")
public class HospitalService {

    private Map<Integer, Appointment> appointments = new HashMap<>();

    public HospitalService() {
        fillCategories();
        HospitalDAO.doctorsList.add((new Doctor("ANURA BANAGALA", "Asiri Hospital", "SURGERY", "9.00 a.m - 11.00 a.m", 7000)));
        HospitalDAO.doctorsList.add((new Doctor("A.N.K. ABAYAJEEWA", "Durdans Hospital", "SURGERY", "8.00 a.m - 10.00 a.m", 12000)));
        HospitalDAO.doctorsList.add((new Doctor("ANIL P AMBAWATTA", "Apollo Hospitals", "SURGERY", "3.00 p.m - 5.00 p.m", 8000)));
        HospitalDAO.doctorsList.add((new Doctor("K. ALAGARATNAM", "Nawaloka Hospital", "CARDIOLOGY", "9.00 a.m - 11.00 a.m", 10000)));
        HospitalDAO.doctorsList.add((new Doctor("SANJAYA ABEYGUNASEKARA", "Asiri Hospital", "CARDIOLOGY", "8.00 a.m - 10.00 a.m", 4000)));
        HospitalDAO.doctorsList.add((new Doctor("K. ALAGARATNAM", "Durdans Hospital", "GYNAECOLOGY", "9.00 a.m - 11.00 a.m", 8000)));
        HospitalDAO.doctorsList.add((new Doctor("SANJAYA ABEYGUNASEKARA", "Asiri Hospital", "GYNAECOLOGY", "8.00 a.m - 10.00 a.m", 11000)));
        HospitalDAO.doctorsList.add((new Doctor("K. ALAGARATNAM", "Asiri Hospital", "ENT", "9.00 a.m - 11.00 a.m", 4500)));
        HospitalDAO.doctorsList.add((new Doctor("SANJAYA ABEYGUNASEKARA", "Asiri Hospital", "ENT", "8.00 a.m - 10.00 a.m", 6750)));
        HospitalDAO.doctorsList.add((new Doctor("AJITH AMARASINGHE", "Durdans Hospital", "PAEDIATRY", "9.00 a.m - 11.00 a.m", 5500)));
        HospitalDAO.doctorsList.add((new Doctor("SANJAYA ABEYGUNASEKARA", "Nawaloka Hospital", "PAEDIATRY", "8.00 a.m - 10.00 a.m", 10000)));
    }

    public void fillCategories() {
        HospitalDAO.catergories.add("SURGERY");
        HospitalDAO.catergories.add("CARDIOLOGY");
        HospitalDAO.catergories.add("GYNAECOLOGY");
        HospitalDAO.catergories.add("ENT");
        HospitalDAO.catergories.add("PAEDIATRY");
    }

    @POST
    @Path("/{category}/reserve")
    public Response reserveAppointment(AppointmentRequest appointmentRequest, @PathParam("category") String category) {

        Gson gson = new Gson();
        // Check whether the requested category available
        if (HospitalDAO.catergories.contains(category)) {
            Appointment appointment = HospitalUtil.makeNewAppointment(appointmentRequest);
            this.appointments.put(appointment.getAppointmentNumber(), appointment);
            HospitalDAO.patientMap.put(appointmentRequest.getPatient().getSsn(), appointmentRequest.getPatient());
            if (!HospitalDAO.patientRecordMap.containsKey(appointmentRequest.getPatient().getSsn())) {
                PatientRecord patientRecord = new PatientRecord(appointmentRequest.getPatient());
                HospitalDAO.patientRecordMap.put(appointmentRequest.getPatient().getSsn(), patientRecord);
            }

            String jsonResponse = gson.toJson(appointment);
            return Response.ok(jsonResponse, MediaType.APPLICATION_JSON).build();
        } else {
            // Cannot find a doctor for this category
            String jsonResponse = "{\"Status\":\"Invalid Category\"}";
            return Response.ok(jsonResponse, MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/appointments/{appointment_id}/fee")
    public Response checkChannellingFee(@PathParam("appointment_id") int id) {
        //Check for the appointment number validity
        Gson gson = new Gson();
        ChannelingFeeDao channelingFee = new ChannelingFeeDao();
        if (appointments.containsKey(id)) {
            Patient patient = appointments.get(id).getPatient();
            Doctor doctor = appointments.get(id).getDoctor();
            int discount = HospitalUtil.checkForDiscounts(patient.getDob());
            double discounted = (((HospitalDAO.findDoctorByName(doctor.getName()).getFee())/100)*(100-discount));

            channelingFee.setActualFee(Double.toString(doctor.getFee()));
            channelingFee.setDiscountedFee(Double.toString(discounted));
            channelingFee.setDiscount(Integer.toString(discount));
            channelingFee.setDoctorName(doctor.getName());
            channelingFee.setPatientName(patient.getName());

            return Response.ok(gson.toJson(channelingFee), MediaType.APPLICATION_JSON).build();
        } else {
            String jsonResponse = "{\"Status\":\"Error.Could not Find the Requested appointment ID\"}";
            return Response.ok(jsonResponse, MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/patient/updaterecord")
    public Response updatePatientRecord(HashMap<String,Object> patientDetails) {
        String SSN = (String)patientDetails.get("SSN");
        List symptoms = (List)patientDetails.get("symptoms");
        List treatments = (List)patientDetails.get("treatments");

        if (HospitalDAO.patientMap.get(SSN) != null) {
            Patient patient = HospitalDAO.patientMap.get(SSN);
            PatientRecord patientRecord = HospitalDAO.patientRecordMap.get(SSN);
            if (patient != null) {
                patientRecord.updateSymptoms(symptoms);
                patientRecord.updateTreatments(treatments);
                return Response.ok("{\"Status\":\"Record Update Success\"}",
                        MediaType.APPLICATION_JSON).build();
            } else {
                return Response.ok("{\"Status\":\"Could not find valid Patient Record\"}",
                        MediaType.APPLICATION_JSON).build();
            }
        } else {
            return Response.ok("{\"Status\":\"Could not find valid Patient Entry\"}",
                    MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/patient/{SSN}/getrecord")
    public Response getPatientRecord(@PathParam("SSN") String SSN) {
        Gson gson = new Gson();
        PatientRecord patientRecord = HospitalDAO.patientRecordMap.get(SSN);

        if (patientRecord == null) {
            return Response.ok("{\"Status\":\"Could not find valid Patient Entry\"}",
                    MediaType.APPLICATION_JSON).build();
        } else {
            return Response.ok(gson.toJson(patientRecord),
                    MediaType.APPLICATION_JSON).build();
        }
    }
}