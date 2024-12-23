package edu.uclm.esi.fakeaccountsbe.http;

import java.util.Collection;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

import edu.uclm.esi.fakeaccountsbe.model.CredencialesRegistro;
import edu.uclm.esi.fakeaccountsbe.model.User;
import edu.uclm.esi.fakeaccountsbe.services.UserService;

@RestController
@RequestMapping("users")
@CrossOrigin("*")
public class UserController {
	@Autowired
	private UserService userService;
	
	@PostMapping("/registrar1")
	public ResponseEntity<Map<String, String>> registrar1(HttpServletRequest req, @RequestBody CredencialesRegistro cr) {
	    cr.comprobar();

	    User user = new User();
	    user.setEmail(cr.getEmail());
	    user.setPwd(cr.getPwd1());
	    this.userService.registrar(req.getRemoteAddr(), user);

	    // Generar token para el usuario registrado
	    String token = UUID.randomUUID().toString();
	    user.setToken(token);
	    this.userService.save(user);

	    // Retornar el token en la respuesta
	    Map<String, String> response = new HashMap<>();
	    response.put("message", "Usuario registrado con éxito.");
	    response.put("token", token);
	    return ResponseEntity.ok(response);
	}


    
    @PostMapping("/recover-password")
    public ResponseEntity<?> recoverPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Correo electrónico es obligatorio");
        }
        // Lógica para generar y enviar el correo con el enlace de recuperación
        boolean success = userService.sendRecoveryEmail(email);
        if (success) {
            return ResponseEntity.ok("Correo enviado correctamente.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Usuario no encontrado.");
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("El correo electrónico es obligatorio.");
        }
        
        boolean result = userService.sendRecoveryEmail(email);
        if (result) {
            return ResponseEntity.ok("Correo de recuperación enviado.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
        }
    }

    
	@GetMapping("/registrar2")
	public void registrar2(HttpServletRequest req, @RequestParam String email, @RequestParam String pwd1, @RequestParam String pwd2) {
		CredencialesRegistro cr = new CredencialesRegistro();
		cr.setEmail(email);
		cr.setPwd1(pwd1);
		cr.setPwd2(pwd2);
		cr.comprobar();
		User user = new User();
		user.setEmail(cr.getEmail());
		user.setPwd(cr.getPwd1());
		
		this.userService.registrar(req.getRemoteAddr(), user);
	}
	
	@GetMapping("/registrarMuchos")
	public void registrarMuchos(HttpServletRequest req, @RequestParam String name, @RequestParam Integer n) {
		for (int i=0; i<n; i++)
			this.registrar2(req, name + i + "@pepe.com", "Pepe1234", "Pepe1234");
	}
	
	@PutMapping("/login1")
	public ResponseEntity<Map<String, String>> login1(@RequestBody User user) {
	    User loggedUser = this.userService.find(user.getEmail(), user.getPwd());
	    loggedUser.setToken(UUID.randomUUID().toString());
	    this.userService.save(loggedUser); // Guarda el token actualizado en la base de datos

	    Map<String, String> response = new HashMap<>();
	    response.put("token", loggedUser.getToken());
	    return ResponseEntity.ok(response);
	}


	@GetMapping("/login2")
	public User login2(HttpServletResponse response, @RequestParam String email, @RequestParam String pwd) {
		User user = this.userService.find(email, pwd);
		user.setToken(UUID.randomUUID().toString());
		response.setHeader("token", user.getToken());
		return user;
	}
	
	@GetMapping("/login3/{email}")
	public User login3(HttpServletResponse response, @PathVariable String email, @RequestParam String pwd) {
		return this.login2(response, email, pwd);
	}
	
	@GetMapping("/getAllUsers")
	public Collection<User> getAllUsers() {
		return this.userService.getAllUsers();
	}
	
	@DeleteMapping("/delete")
	public void delete(HttpServletRequest request, @RequestParam String email, @RequestParam String pwd) {
		User user = this.userService.find(email, pwd);
		
		String token = request.getHeader("token");
		if (!token.equals(user.getToken()))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token " + token + " inválido");
		
		this.userService.delete(email);
	}
	
	@DeleteMapping("/clearAll")
	public void clearAll(HttpServletRequest request) {
		String sToken = request.getHeader("prime");
		Integer token = Integer.parseInt(sToken);
		if (!isPrime(token.intValue()))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes pasar un número primo en la cabecera");
		if (sToken.length()!=3)
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El nº primo debe tener tres cifras");
		this.userService.clearAll();
	}
	
	private boolean isPrime(int n) {
	    if (n <= 1) return false;
	    for (int i = 2; i <= Math.sqrt(n); i++) {
	        if (n % i == 0) return false;
	    }
	    return true;
	}
}
















