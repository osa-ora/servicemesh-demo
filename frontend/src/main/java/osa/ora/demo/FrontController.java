package osa.ora.demo;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/front")
public class FrontController {

	@Value("${end_point}")
    private String endPoint;
	private final RestTemplate restTemplate;

	@Autowired
    public FrontController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

	@GetMapping(path = "/test/{account}")
    public HashMap testApp(@PathVariable(value = "account") Integer account) {
		String apiUrl = endPoint+account;
		System.out.println("End point:"+apiUrl);
        String response = "";
	ResponseEntity<String> restResponse = restTemplate.getForEntity(apiUrl, String.class); // use getForEntity to get ResponseEntity
	
	if (restResponse.getStatusCode() == HttpStatus.OK) {
	    response = restResponse.getBody();
	} else {
	    response = "Error: " + restResponse.getStatusCode() + " " + restResponse.getStatusCode().getReasonPhrase();
	    // Handle based on status code if needed
	}
	System.out.println("Get Current Balance for account from url: " + response);

        return new HashMap(){{
            put("Welcome"," guest");
            put("Response:",response);
        }};
    }

	@GetMapping(path = "/")
    public HashMap homePage() {
		System.out.println("Home page");
        return new HashMap(){{
            put("Welcome"," guest");
        }};
    }

}
