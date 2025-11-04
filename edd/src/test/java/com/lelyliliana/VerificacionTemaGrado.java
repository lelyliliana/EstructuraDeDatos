package com.lelyliliana;

import java.util.Scanner;

public class VerificacionTemaGrado { 
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in); 

        //Datos de entrada 
        System.out.println("== Verificación para tema de grado==");
        System.out.println("Ingrese el número total de créditos del programa: ");
        int totalCreditos = sc.nextInt();

        System.out.println("Ingrese el número de créditos aprobados: ");
        int creditosAprobados = sc.nextInt();

        System.out.println("Ingrese su promedio acumulado: ");
        double promedio = sc.nextDouble();

        System.out.println("¿Tienes sanciones disciplinarias activas? (true/false)");
        boolean sancion = sc.nextBoolean();

        //condiciones de aprobación 
        boolean cumpleCreditos = creditosAprobados>=(0.80*totalCreditos); //80% aprobados
        boolean cumplePromedio = promedio >= 3.0; //promedio mínimo
        boolean sinSanciones = !sancion;
        
        //EValuación 
        if(cumpleCreditos && cumplePromedio && sinSanciones) {
            System.out.println("Cumple requisitos para inscribir el tema de grado.");
        } else {
            System.out.println("No cumple con los requisitos. Detalles:");
            if(!cumpleCreditos) { 
                System.out.println("- +No ha aprobado al menos el 80% de los créditos");
            }; 
        }
        if (!cumplePromedio) {
            System.out.println("- El promedio es inferior a 3.0");
        }
        if (!sinSanciones) {
            System.out.println("- Tienes sanciones disciplinarios activas");            
        }
        }
    }