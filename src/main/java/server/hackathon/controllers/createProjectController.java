package server.hackathon.controllers;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import server.hackathon.dto.CreateProjectBodyDTO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController

public class createProjectController
{

    @PostMapping (path = "api/v1/createproject")
    @CrossOrigin()
    public ResponseEntity<String> createReactProject (@RequestBody CreateProjectBodyDTO requestInput){


        AtomicInteger counter = new AtomicInteger(0); // variable de multithreading (compartida en memoria)
        requestInput.getSpa().parallelStream().forEach(req -> {
            List<String> command = List.of("C:/Program Files/nodejs/node.exe", "C:/Program Files/nodejs/node_modules/npm/bin/npx-cli.js", "create-react-app", req.getName() );
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            //La parte del directorio se tiene que cambiar porque depende del usuario y el ordenador
            processBuilder.directory(new File("C:/Users/Anas/Desktop/techSumit/v4"));

            try {
                int exitCode = processBuilder.start().waitFor();
                if (exitCode == 0) {
                    counter.addAndGet(1);
                    req.getRelations().parallelStream().forEach(rel -> {
                        if( rel.getTarget().contains("spa") ) {
                            String jsCode = getRouterTemplate(rel.getTarget());
                            //La parte del directorio se tiene que cambiar porque depende del usuario y el ordenador
                            String directory = "C:/Users/Anas/Desktop/techSumit/v4/"+rel.getSource()+"/src";
                            createJsFile(directory, "navigateTo"+rel.getTarget()+".js", jsCode );
                        }
                        else if(rel.getTarget().contains("api")) {
                           // String jsCode = getApiCallTemplate();
                            req.getApis().forEach(api -> {
                                String apiName = api.getName();
                                if (apiName.equals(rel.getTarget()) ) {
                                    String jsCode = getMock(api.getMock());
                                    //La parte del directorio se tiene que cambiar porque depende del usuario y el ordenador
                                    String directory = "C:/Users/Anas/Desktop/techSumit/v4/"+rel.getSource()+"/src";
                                    createJsFile(directory, api.getName()+api.getMethod()+"Mock.json", jsCode );
                                    System.out.println("Se ha creado el mock");
                                    jsCode = getApiCallTemplate(rel.getTarget(),api.getEndpoint(), api.getMethod(), api.getBody());
                                    createJsFile(directory, "use"+rel.getTarget()+api.getMethod()+".js", jsCode );
                                }
                            });
                        }
                    });
                }

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

         return  (requestInput.getSpa().size() == counter.get())? ResponseEntity.ok("React projects created successfully."): ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body("Failed to create React projects.");
    }

    public void createJsFile ( String directory, String fileName, String jsCode) {
        File jsFile = new File(directory, fileName);
        try (FileWriter fileWriter = new FileWriter(jsFile)) {
            fileWriter.write(jsCode);
            System.out.println("JavaScript file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to create JavaScript file.");
        }
    }

    public String getRouterTemplate (String target) {
        String jsCode = "import { router } from '@absis/core';\n" +"\n"+
                "export const navigate = () => {\n" +
                "let navigationManager = router.navigationManager();\n" +
                "router.navigateTo('/"+ target + "', { type: 'External' }, navigationManager);\n" +
                "}";

        return jsCode;
    }

    public String getApiCallTemplate(String target, String endpoint, String method, String body) {
        String parsedBody;
        if ( !body.equals("{}"))  {parsedBody = new JSONObject(body).toString();}
        else {parsedBody = "";}

        // hay que añadir el body y el pathParams
        String jsCode = "import { http } from '@absis/core';\n" +
                "import { useState, useEffect } from 'react';\n"+
                "export const use"+target+method+" = (url,body,Qparams, Pparams) => {\n" +
                "//bodyExample = "+ parsedBody+"\n"+
                "const [data, setData] = useState(null);\n" +
                "const [error, setError] = useState(null);\n"+
                "useEffect(() => {\n"+
                "      setData(null);\n" +
                "      setError(null);"+
                "      http.call('"+endpoint+"',{pathParams: {...Pparams}},{queryParams:{...Qparams}} ,{data:{...body}}).then(res => {\n"+
                "      if ([200, 204].includes(res.status)) {\n" +
                "       setData(res.data);\n" +
                "       setError(false);\n"+
                "      } else {\n" +
                "          setError(true);\n" +
                "        }\n"+
                "      })\n"+
                "       .catch(err =>{\n"+
                "          setError('An error occured')\n" +
                "        })"+
                "},[url])\n"+
                "return {data,error}\n" +
                "}";
        return jsCode;
    }

    public String getMock ( String response) {
        JSONObject parsedResponse =   new JSONObject(response);
        String jsCode = ""+parsedResponse.toString();
        return jsCode;
    }
}
