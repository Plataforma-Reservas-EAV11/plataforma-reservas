package com.udea.reservas.plataformareservas.auth.service;

import com.udea.reservas.plataformareservas.auth.dto.UserRegistrationRequest;
import com.udea.reservas.plataformareservas.usuarios.model.User;

public interface UserService {

    User register(UserRegistrationRequest registrationRequest);
}
