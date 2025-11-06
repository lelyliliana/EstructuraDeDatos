package com.lelyliliana;

import java.util.*;
import java.util.stream.Collectors;

public class KruskalTerminalGame {

    // ======= Modelo de datos =======
    static class Edge implements Comparable<Edge> {
        int id;
        int u, v;
        int w;

        Edge(int id, int u, int v, int w) {
            this.id = id; this.u = u; this.v = v; this.w = w;
        }

        @Override public int compareTo(Edge o) {
            if (this.w != o.w) return Integer.compare(this.w, o.w);
            // desempatar por endpoints para estabilidad
            if (this.u != o.u) return Integer.compare(this.u, o.u);
            return Integer.compare(this.v, o.v);
        }

        @Override public String toString() {
            return String.format("#%02d  (%d — %d)  w=%d", id, u, v, w);
        }
    }

    static class DSU {
        int[] p, r;
        DSU(int n) {
            p = new int[n]; r = new int[n];
            for (int i=0;i<n;i++){p[i]=i;r[i]=0;}
        }
        int find(int x){ return p[x]==x?x:(p[x]=find(p[x])); }
        boolean same(int a,int b){ return find(a)==find(b); }
        boolean union(int a,int b){
            a=find(a); b=find(b);
            if(a==b) return false;
            if(r[a]<r[b]){int t=a;a=b;b=t;}
            p[b]=a;
            if(r[a]==r[b]) r[a]++;
            return true;
        }
        Map<Integer,List<Integer>> components(){
            Map<Integer,List<Integer>> m=new HashMap<>();
            for(int i=0;i<p.length;i++){
                int f=find(i);
                m.computeIfAbsent(f,k->new ArrayList<>()).add(i);
            }
            return m;
        }
    }

    static class Graph {
        int n;
        List<Edge> edges = new ArrayList<>();
        Graph(int n){ this.n=n; }
    }

    // ======= Utilidades =======
    static Graph randomConnectedGraph(int n, int extraEdges, int minW, int maxW, Random rnd){
        Graph g = new Graph(n);
        int eid=1;

        // Primero: un árbol aleatorio para garantizar conectividad
        for(int v=1; v<n; v++){
            int u = rnd.nextInt(v);
            int w = rnd.nextInt(maxW-minW+1)+minW;
            g.edges.add(new Edge(eid++, u, v, w));
        }
        // Agregar aristas extra evitando duplicados y lazos
        Set<Long> seen = new HashSet<>();
        for (Edge e : g.edges) {
            long key = key(e.u, e.v);
            seen.add(key);
        }
        int attempts = 0;
        while (extraEdges>0 && attempts<1000){
            int u = rnd.nextInt(n), v = rnd.nextInt(n);
            if(u==v) { attempts++; continue; }
            long k = key(u,v);
            if(seen.contains(k)) { attempts++; continue; }
            seen.add(k);
            int w = rnd.nextInt(maxW-minW+1)+minW;
            g.edges.add(new Edge(eid++, Math.min(u,v), Math.max(u,v), w));
            extraEdges--;
        }
        return g;
    }
    static long key(int a,int b){
        int x=Math.min(a,b), y=Math.max(a,b);
        return (((long)x)<<32) | (long)y;
    }

    static class KruskalResult {
        List<Edge> mst = new ArrayList<>();
        int totalWeight = 0;
        List<String> log = new ArrayList<>();
    }

    static KruskalResult runKruskal(Graph g){
        KruskalResult res = new KruskalResult();
        DSU dsu = new DSU(g.n);
        List<Edge> sorted = new ArrayList<>(g.edges);
        Collections.sort(sorted);

        for (Edge e : sorted){
            boolean same = dsu.same(e.u, e.v);
            res.log.add(String.format("Considerando %s  | %s",
                    e, same ? "RECHAZAR (forma ciclo)" : "ACEPTAR"));
            if (!same){
                dsu.union(e.u, e.v);
                res.mst.add(e);
                res.totalWeight += e.w;
                if (res.mst.size()==g.n-1) break;
            }
        }
        return res;
    }

    // ======= Juego interactivo =======
    static void play(Scanner sc, Graph g){
        System.out.println("=== JUEGO: Kruskal en la terminal ===");
        System.out.println("Objetivo: decidir si cada arista se (s)elecciona o (n)o se selecciona");
        System.out.println("Regla: Selecciona la arista si NO forma ciclo con las ya elegidas.");
        System.out.println();

        // Mostrar grafo
        System.out.printf("Grafo con %d vértices (0..%d) y %d aristas:%n", g.n, g.n-1, g.edges.size());
        for (Edge e : g.edges) System.out.println(e);
        System.out.println();

        // Ordenar por peso para simular el orden de Kruskal
        List<Edge> sorted = new ArrayList<>(g.edges);
        Collections.sort(sorted);
        DSU dsu = new DSU(g.n);
        List<Edge> chosen = new ArrayList<>();
        int mistakes = 0;

        for (Edge e : sorted){
            if (chosen.size()==g.n-1) break;

            // Mostrar componentes actuales
            Map<Integer,List<Integer>> comps = dsu.components();
            String compStr = comps.values().stream()
                    .sorted(Comparator.comparingInt(a->a.get(0)))
                    .map(ls -> ls.stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .collect(Collectors.joining(" | "));
            System.out.println("Componentes actuales: ["+compStr+"]");

            System.out.println("Siguiente por peso -> " + e);
            boolean wouldCycle = dsu.same(e.u, e.v);

            System.out.print("¿(s)eleccionar o (n)o seleccionar? ");
            String ans = sc.nextLine().trim().toLowerCase();
            while (!(ans.equals("s") || ans.equals("n"))) {
                System.out.print("Responde 's' o 'n': ");
                ans = sc.nextLine().trim().toLowerCase();
            }

            if (ans.equals("s")){
                if (wouldCycle){
                    System.out.println("✗ Incorrecto: esa arista forma ciclo. Debías rechazarla.");
                    mistakes++;
                } else {
                    dsu.union(e.u, e.v);
                    chosen.add(e);
                    System.out.println("✓ Correcto: añadida al bosque generador.");
                }
            } else { // 'n'
                if (wouldCycle){
                    System.out.println("✓ Correcto: rechazada porque formaba ciclo.");
                } else {
                    System.out.println("✗ Incorrecto: esa arista debía aceptarse (no formaba ciclo).");
                    mistakes++;
                }
            }
            System.out.println();
        }

        int totalW = chosen.stream().mapToInt(ed->ed.w).sum();
        KruskalResult optimal = runKruskal(g);

        System.out.println("==== RESULTADOS ====");
        System.out.println("Tus aristas ("+chosen.size()+") y peso total = "+totalW);
        for (Edge e : chosen) System.out.println("  " + e);
        System.out.println();
        System.out.println("MST óptimo ("+optimal.mst.size()+") y peso total = " + optimal.totalWeight);
        for (Edge e : optimal.mst) System.out.println("  " + e);
        System.out.println();
        System.out.println("Errores cometidos: " + mistakes);
        if (chosen.size()==g.n-1 && totalW==optimal.totalWeight){
            System.out.println("🎉 ¡Perfecto! Construiste un MST óptimo.");
        } else if (chosen.size()==g.n-1){
            System.out.println("Bien: construiste un árbol generador, pero no fue óptimo en peso.");
        } else {
            System.out.println("El árbol no quedó completo. Repasa cuándo se forma un ciclo.");
        }
        System.out.println("====================");
    }

    static void demo(Graph g){
        System.out.println("=== DEMO PASO A PASO (Kruskal automático) ===");
        KruskalResult r = runKruskal(g);
        for (String line : r.log) System.out.println(line);
        System.out.println("Peso total del MST: " + r.totalWeight);
        System.out.println("Aristas en el MST:");
        for (Edge e : r.mst) System.out.println("  " + e);
        System.out.println("====================");
    }

    // ======= Main / menú =======
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("KruskalTerminalGame — Estructuras de Datos");
        System.out.println("1) Jugar con grafo aleatorio");
        System.out.println("2) Demo automática (paso a paso)");
        System.out.println("3) Ingresar grafo manualmente");
        System.out.println("0) Salir");
        System.out.print("Elige una opción: ");

        String op = sc.nextLine().trim();

        switch (op){
            case "1": {
                Random rnd = new Random();
                int n = 6 + rnd.nextInt(3);          // 6..8 vértices
                int extra = 4 + rnd.nextInt(4);      // +4..+7 aristas extra
                Graph g = randomConnectedGraph(n, extra, 1, 20, rnd);
                play(sc, g);
                break;
            }
            case "2": {
                Random rnd = new Random();
                int n = 6 + rnd.nextInt(3);
                int extra = 4 + rnd.nextInt(4);
                Graph g = randomConnectedGraph(n, extra, 1, 20, rnd);
                System.out.printf("Grafo con %d vértices y %d aristas%n", g.n, g.edges.size());
                for (Edge e : g.edges) System.out.println(e);
                System.out.println();
                demo(g);
                break;
            }
            case "3": {
                Graph g = readGraph(sc);
                System.out.println();
                System.out.println("1) Jugar");
                System.out.println("2) Demo automática");
                System.out.print("Elige: ");
                String o2 = sc.nextLine().trim();
                if ("1".equals(o2)) play(sc, g); else demo(g);
                break;
            }
            default:
                System.out.println("¡Hasta luego!");
        }
    }

    // Carga manual: numeración de vértices 0..n-1
    static Graph readGraph(Scanner sc){
        System.out.print("Número de vértices (n): ");
        int n = readInt(sc);
        System.out.print("Número de aristas (m): ");
        int m = readInt(sc);
        Graph g = new Graph(n);
        for (int i=1;i<=m;i++){
            System.out.printf("Arista #%d (u v w): ", i);
            String[] parts = sc.nextLine().trim().split("\\s+");
            while (parts.length!=3){
                System.out.print("Ingresa exactamente tres enteros 'u v w': ");
                parts = sc.nextLine().trim().split("\\s+");
            }
            int u = Integer.parseInt(parts[0]);
            int v = Integer.parseInt(parts[1]);
            int w = Integer.parseInt(parts[2]);
            if (u<0||u>=n||v<0||v>=n||u==v){
                System.out.println("Valores fuera de rango o lazo; intenta de nuevo.");
                i--; continue;
            }
            g.edges.add(new Edge(i, Math.min(u,v), Math.max(u,v), w));
        }
        return g;
    }

    static int readInt(Scanner sc){
        while (true){
            String s = sc.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (Exception e){ System.out.print("Ingresa un entero válido: "); }
        }
    }
}

