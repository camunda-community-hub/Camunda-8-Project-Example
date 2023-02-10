package io.camunda.getstarted.genericWorker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableZeebeClient
public class Worker {

    public static void main(String[] args) {
        SpringApplication.run(Worker.class, args);
    }

    //{"complexity":"High"}

    @JobWorker(type = "DoWork")
    public void DoWork(final JobClient client, final ActivatedJob job) {
        


        Map<String, Object> variablesAsMap = job.getVariablesAsMap();
        Boolean throwError = (Boolean) variablesAsMap.get("throwError");
        
        try {
            System.out.println("Lets find out if i can throw an error "+ throwError);
           
            if(throwError == null || !throwError ) {
                
                HashMap<String, Object> variables = new HashMap<>();
                variables.put("variable", "Value");
               

                System.out.println("Going to complete the task now... i hope");

                client.newCompleteCommand(job.getKey())
                        .variables(variables)
                        .send()
                        .exceptionally((throwable -> {
                            throw new RuntimeException("Could not complete job", throwable);
                        }));
            } else {
                System.out.println("Going to throw an error now... i think");
                client.newThrowErrorCommand(job.getKey())
                        .errorCode("Problem")
                        .errorMessage("Something bad happened and it was your fault")
                        .send()
                        .exceptionally((throwable -> {
                            throw new RuntimeException("Could not throw the BPMN Error Event", throwable);
                        }));
            }
        } catch(Exception e) {
            int retries = job.getRetries() - 1;

            e.printStackTrace();
            client.newFailCommand(job.getKey())
                .retries(retries)
                .send();
        }
    }
    
    @JobWorker(type = "DoLongWork")
    public void DoMoreWork(final JobClient client, final ActivatedJob job) {
        
        Map<String, Object> variablesAsMap = job.getVariablesAsMap();
        Integer mins = (Integer) variablesAsMap.get("minutes");
        if(mins == null){
            mins = 1;
        }

        System.out.println("This is going to take "+ mins + " minute(s) to execute");

        try{       
            TimeUnit.MINUTES.sleep(mins);
        }catch(Exception e){
            System.out.println("Timing issue");
        }   
                client.newCompleteCommand(job.getKey())
                        .send()
                        .exceptionally((throwable -> {
                            throw new RuntimeException("Could not complete job", throwable);
                        }));
    }
}
