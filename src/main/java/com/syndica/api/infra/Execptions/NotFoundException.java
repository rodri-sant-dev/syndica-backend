package com.syndica.api.infra.Execptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message){
        super(message);
    }
}
