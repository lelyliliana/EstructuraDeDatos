package com.lelyliliana;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import static java.lang.Math.*;

public class GradoDijkstra {

    //parámetros ajustables 
    private static final double PORC_UMBRAL_CREDITOS = 0.80; 
    private static final int CREDITOS_POR_SEMESTRE = 18; 
    private static final double COSTO_SUBIR_PROMEDIO = 1.0;
    private static final double COSTO_LEVENTAR_SANCION = 0.5; 

    //bits requisitos 
    private static final int CRED_OK = 1 << 0; //001 
    private static final int PROM_OK = 1 << 1; //010
    private static final int SAN_OK = 1 << 2; //100
    private static final int OBJETIVO = CRED_OK | PROM_OK | SAN_OK; //111
    
    //Estructura para Dijkstra 
    static class Edge { 
        int to; 
        double w; 
        String accion; //describir la acción 
        Edge(int to, double w, String accion) {
            this.to = to; this.w=w; this.accion=accion; }
    }

    static class Node implements Comparable<Node> { 
        int v; 
        double dist; 
        Node(int v, double dist) {
            this.v=v; this.dist=dist;
        }
        public int compateTo(Node o) {
            return Double.compare(this.dist,o.dist); 
        }
    }
public static void main(String[] args) { 
    Scanner sc = new Scanner(System.in); 

    System.out.println("==Verificar tema grado con Dijkstra==");
    System.out.println("Ingrese el número total de créditos del programa: ");
    int totalCreditos = sc.nextInt(); 

    System.out.println("Ingrese el número de créditos aprobados: ");
    int creditosAprobados = sc.nextInt();

    System.out.println("Ingrese su promedio acumulado: ");
    double promedio = sc.nextDouble();

    System.out.println("¿Tienes sanciones activas? (true/false): ");
    boolean sancionActiva = sc.nextBoolean();

    //1. Determinar el estado inicial (bitmask)
    boolean cumpleCreditos = creditosAprobados >= (PORC_UMBRAL_CREDITOS*totalCreditos);
    boolean cumplePromedio = promedio >=3.0; 
    boolean sinSanciones = !sancionActiva;

    int estadoInicial = 0; 
    if(cumpleCreditos) estadoInicial |= CRED_OK;
    if(cumplePromedio) estadoInicial |= PROM_OK; 
    if(sinSanciones) estadoInicial |=SAN_OK;

    List<List<Edge>> g = new ArrayList<>();
    for(int i = 0; i < 8; i++) g.add(new ArrayList<>());

    for(int s = 0; s < 9; s++) { 

        //créditos
        if((s & CRED_OK) == 0) { 
            double costo = costoCumplirCreditos(totalCreditos, creditosAprobados);
            int t = s | CRED_OK;
            String accion = describirAccionCreditos(totalCreditos, creditosAprobados);
            g.get(s).add(new Edge(t, costo, accion));
        }

        //promedio
        if((s & PROM_OK) == 0) { 
            if (!cumplePromedio) { 
                double costo = COSTO_SUBIR_PROMEDIO;
                int t = s | PROM_OK; 
                String accion = "Mejorar promedio a >=3.0 (Plan nivelación)";
                g.get(s).add(new Edge(t, costo, accion));
            } else {
                g.get(s).add(new Edge(s | PROM_OK, 0.0, "Ya cumple promedio (acción costo 0)."));
            }
        }

        //sanciones
        if((s & SAN_OK) == 0) { 
            if (!sinSanciones) { 
                double costo = COSTO_LEVENTAR_SANCION; 
                int t = s | SAN_OK;
                String accion = "Levantar sanción";
                g.get(s).add(new Edge(t, costo, accion));
            } else { 
                g.get(s).add(new Edge(s | SAN_OK, 0.0,"Sin sanciones"));
            }
        }
    }

    //Dijkstra 
    double[] dist = new double[8];
    int[] parent = new int[8];
    String[] viaAccion = new String[8];
    Arrays.fill(dist, Double.POSITIVE_INFINITY);
    Arrays.fill(parent, -1);

    PriorityQueue<Node> pq = new PriorityQueue<>();
    dist[estadoInicial] = 0.0;
    pq.add(new Node(estadoInicial,0.0));

    while (!pq.isEmpty()) {
        Node cur = pq.poll();
        if(cur.dist != dist[cur.v]) continue;
        if(cur.v == OBJETIVO) break;

        for(Edge e : g.get(cur.v)) { 
            if(dist[e.to] > dist[cur.v] + e.w) { 
                dist[e.to] = dist[cur.v] + e.w;
                parent[e.to] = cur.v; 
                viaAccion[e.to] = e.accion;
                pq.add(new Node(e.to, dist[e.to]));
            }
        }
        
    }
    //reportes
    System.out.println("Resultado de la verificación");
    if(estadoInicial == OBJETIVO) {
        System.out.println("Cumple con los requisitos de tema de grado");
    } else {
        System.out.println("No cumples con los requisitos del tema de grado");
    }

    reportarDetalleRequisitos(cumpleCreditos, cumplePromedio, sinSanciones);
    
    System.out.println("Plan mínimo Dijkstra");
    if(dist[OBJETIVO]==Double.POSITIVE_INFINITY) { 
        System.out.println("No fue posible construir un plan de los supuestos actuales.");
    } else {
        List<Integer> ruta = reconstruirRuta(parent,OBJETIVO);
        int prev = estadoInicial; 
        for(int i = ruta.size()-1;i>=0; i--) {
            int v = ruta.get(i);
            if(v == estadoInicial) continue;
            String accion = viaAccion[v];
            double paso = dist[v] - dist[prev];
            System.out.printf("- %s (costo: %.2f semestres)\n", accion, paso);
            prev = v;          
        } 
        System.out.printf("Costo total estimado: %.2f semestres.\n",dist[OBJETIVO]);
    }
    
    }

    //Utilidades del modelo 

private static List<Integer> reconstruirRuta(int[] parent, int objetivo) {
    List<Integer> ruta = new ArrayList<>();
    for(int v = objetivo; v != -1; v=parent[v]) ruta.add(v);
    return ruta; 
}
private static void reportarDetalleRequisitos(boolean cred, boolean prom, boolean sanc) {
    System.out.println("Requisitos actuales: ");
    System.out.println(" - Créditos: " + (cred ? "Cumple" : "No cumple"));
    System.out.println(" - Promedio: " + (prom ? "Cumple" : "No Cumple (debe ser >= 3.0)"));
    System.out.println(" - Sanciones: " + (sanc ? "Sin sanciones" : "Tiene sanciones activas"));
}
private static double costoCumplirCreditos(int totalCred, int aprob) {
    int minNecesarios = (int) ceil(PORC_UMBRAL_CREDITOS * totalCred);
    int faltantes = max(0, minNecesarios - aprob);
    if(faltantes == 0) return 0.0;
    return ceil((double) faltantes / CREDITOS_POR_SEMESTRE);
}
private static String describirAccionCreditos(int totalCred, int aprob) {
    int minNecesarios = (int) ceil(PORC_UMBRAL_CREDITOS * totalCred);
    int faltantes = max(0, minNecesarios - aprob);
    if(faltantes <= 0) { 
        return "Créditos ya cumplen";
    }
    return "Aprobar créditos faltantes (" + faltantes + "cr) hasta alcanzar >= " 
    + (int)(PORC_UMBRAL_CREDITOS*100) + "% del programa."; 

}

}
