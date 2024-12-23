package edu.uclm.esi.fakeaccountsbe.services;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import edu.uclm.esi.fakeaccountsbe.model.User;
import edu.uclm.esi.fakeaccountsbe.repositories.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class UserService {
		
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JavaMailSender mailSender;
    
	private Map<String, User> users = new ConcurrentHashMap<>();
	private Map<String, List<User>> usersByIp = new ConcurrentHashMap<>();

    public void registrar(String ip, User user) {
        if (userRepository.existsById(user.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con ese correo electrónico");
        }

        user.setIp(ip);
        user.setCreationTime(System.currentTimeMillis());
        userRepository.save(user); // Guardar el usuario en la base de datos
        System.out.println("Usuario registrado exitosamente: " + user.getEmail());
    }


	public void login(User tryingUser) {
		this.find(tryingUser.getEmail(), tryingUser.getPwd());
	}

	public void clearAll() {
		this.usersByIp.clear();
		this.users.clear();
	}

	public Collection<User> getAllUsers() {
		return this.users.values();
	}
	
	public boolean sendRecoveryEmail(String email) {
	    User user = userRepository.findById(email).orElse(null);
	    if (user == null) {
	        return false; // Usuario no encontrado
	    }

	    // Generar token de recuperación
	    String recoveryToken = java.util.UUID.randomUUID().toString();
	    user.setToken(recoveryToken);
	    userRepository.save(user);

	    // URL de recuperación
	    String recoveryUrl = "http://localhost:4200/reset-password?token=" + recoveryToken;

	    try {
	        // Enviar correo
	        sendEmail(email, recoveryUrl);
	        return true;
	    } catch (MessagingException e) {
	        e.printStackTrace();
	        return false;
	    }
	}


	private void sendEmail(String to, String recoveryUrl) throws MessagingException {
	    MimeMessage message = mailSender.createMimeMessage();
	    MimeMessageHelper helper = new MimeMessageHelper(message, true);

	    helper.setTo(to);
	    helper.setSubject("Recuperación de contraseña");
	    helper.setText(
	        "<p>Para restablecer tu contraseña, haz clic en el siguiente enlace:</p>" +
	        "<a href='" + recoveryUrl + "'>Restablecer Contraseña</a>",
	        true
	    );

	    mailSender.send(message);
	}

	
    public User find(String email, String pwd) {
        User user = userRepository.findById(email).orElse(null);
        if (user == null || !user.getPwd().equals(pwd)) {
            throw new RuntimeException("Credenciales incorrectas");
        }
        return user;
    }
	public void delete(String email) {
		User user = this.users.remove(email);
		List<User> users = this.usersByIp.get(user.getIp());
		users.remove(user);
		if (users.isEmpty())
			this.usersByIp.remove(user.getIp());
	}
    public void save(User user) {
        userRepository.save(user); // Este método ahora usa el repositorio para guardar los cambios
    }

	public synchronized void clearOld() {
		long time = System.currentTimeMillis();
		for (User user : this.users.values())
			if (time> 600_000 + user.getCreationTime())
				this.delete(user.getEmail());
	}

}














