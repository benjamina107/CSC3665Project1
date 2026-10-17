package com.csc3665.project1.dto;

public record PTOResponse (
    PTOBalance vacation,
    PTOBalance sick
){
public record PTOBalance(int used, int total){}
}
