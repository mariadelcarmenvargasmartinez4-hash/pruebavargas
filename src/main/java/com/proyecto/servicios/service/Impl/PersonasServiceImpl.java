package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PersonasServiceImpl implements PersonaService {

    private final PersonasRepository personasRepository;

    public PersonasServiceImpl(PersonasRepository personasRepository) {
        this.personasRepository = personasRepository;
    }

    @Override
    public PersonaResponse creaPersona(PersonasRequest personasRequest) {
        Personas persona = new Personas();
        persona.setNombre(personasRequest.getNombre());
        persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
        persona.setApellidoP(personasRequest.getApellidoP());
        personasRepository.save(persona);

        PersonaResponse person = new PersonaResponse();
        person.setCodigo(1);
        person.setMensaje("Exito");
        BeanUtils.copyProperties(persona, person);
        return person;
    }

    @Override
    public GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest) {
        return personasRepository.findByNombre(eliminaPersonaRequest.getNombre())
                .map(personaElimina -> {
                    personasRepository.delete(personaElimina);
                    GenericResponse response = new GenericResponse();
                    response.setCodigo(0);
                    response.setMensaje("La persona ha sido eliminada correctamente");
                    return response;
                })
                .orElseGet(() -> {
                    GenericResponse response = new GenericResponse();
                    response.setCodigo(1);
                    response.setMensaje("La persona no existe ");
                    return response;
                });
    }

    @Override
    public GenericResponse actualizaPersona(PersonasRequest personasRequest) {
        return personasRepository.findByNombre(personasRequest.getNombre())
                .map(personaActualiza -> {
                    personaActualiza.setApellidoP(personasRequest.getApellidoP());
                    personaActualiza.setApellidoMaterno(personasRequest.getApellidoMaterno());
                    personasRepository.save(personaActualiza);
                    GenericResponse response = new GenericResponse();
                    response.setCodigo(0);
                    response.setMensaje("la persona ha sido actualizada correctamente");
                    return response;
                })
                .orElseGet(() -> {
                    GenericResponse response = new GenericResponse();
                    response.setCodigo(1);
                    response.setMensaje("La persona no existe ");
                    return response;
                });
    }
}
