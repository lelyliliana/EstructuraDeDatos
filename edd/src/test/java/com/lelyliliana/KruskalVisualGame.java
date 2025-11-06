package com.lelyliliana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class KruskalVisualGame {

    static class Edge implements Comparable<Edge> {
    int id, u, v, w;
    Edge(int id, int u, int v, int w) { 
        this.id=id;
        this.u=u; 
        this.v=v; 
        this.w=w; 
    }
    public int CompareTo(Edge o) { return Integer.compare(this.w, o.w); }
    @Override public String toString() { 
        return String.format("#%02d (%d - %d) w=%d", id, u, v, w); 
    }
    }

    static class DSU { 
        int[] p, r; 
        DSU(int n) { p=new int [n]; r=new int[n];
        for(int i=0; i<n;i++) p[i]=i; }
        int find(int x) { 
            return p[x]==x?x:(p[x]=find(p[x])); }
        boolean same(int a, int b) {
            return find(a)==find(b);
        }
        boolean union(int a, int b) { 
            a=find(a); 
            b=find(b);
            if(a==b) return false; 
            if(r[a]<r[b]) {int t=a;a=b;b=t; }
            p[b]=a; 
            if(r[a]==r[b]) r[a]++;
            return true;
        }
        Map<Integer,List<Integer>> components() { 
            Map<Integer,List<Integer>> m=new HashMap<>();
            for(int i=0; i<p.length;i++) { 
                int f=find(i);
                m.computeIfAbsent(f,k -> new ArrayList<>()).add(i);
            }
            return m;
        }
    }
    static Graph randomConnectedGraph(int n, int extra, int minW, int maxW, Random rnd){
        Graph g = new Graph(n); 
        int eid=1; 

        for(int v=1;v<n;v++) {
            int u=rnd.nextInt(v);
            int w = rnd.nextInt(maxW-minW+1)+minW;
            g.edges.add(new Edge(eid++, u, v, w));
        }
        Set<Long> seen=new HashSet<>();
        for(Edge e:g.edges) seen.add(key(e.u,e.v));
        while (extra>0) {
            int u =rnd.nextInt(n),v=rnd.nextInt(n);
            if(u==v)continue;
            long k=key(u,v);
            if(seen.contains(k))continue;
            seen.add(k);
            int w=rnd.nextInt(maxW-minW+1)+minW;
            g.edges.add(new Edge(eid++, Math.min(u, w), Math.max(u,v),w));
            extra--;
        }
        return g; 
    }
    static Long key(int a, int b) {
        return (((long)Math.min(a, b))<<32) | Math.max(a,b);
    }

    static class Graph {
        int n; 
        List<Edge> edges=new ArrayList<>();
        Graph(int n) {
            this.n=n;
        }     

        public static void main(String[] args) {
            Scanner sc = new Scanner(System.in); 
            Random rnd = new Random(); 
            Graph g = randomConnectedGraph(6, 5, 1, 20, rnd);
            play(g, sc);
        }

        static void play(Graph g, Scanner sc) {
            System.out.println("===================================");
            System.out.println("     JUEGO ALGORITMO KRUSKAL        ");
            System.out.println("===================================");
            System.out.println("Reglas");
            System.out.println("- Selecciona (s) si la arista NO forma un ciclo.");
            System.out.println("- No selecciones (n) si YA une vértices del mismo grupo.\n");

            System.out.printf("Grafo con %d vértices y %d aristas:\n",g.n,g.edges.size());
            for(Edge e:g.edges) System.out.println(" "+ e);
            System.out.println();

            List<Edge> sorted = new ArrayList<>(g.edges);
            Collectors.sort(sorted);
            DSU dsu = new DSU(g.n);
            List<Edge> chosen = new ArrayList<>();
            int mistakes = 0; 
            int step = 1;

            while(chosen.size() < g.n - 1 && step <= sorted.size()) { 
                Edge e = sorted.get(step -1);

                Map<Integer,List<Integer>> comps = dsu.components();
                char groupChar = 'A';
                StringBuilder compText = new StringBuilder();
                for(var entry : comps.entrySet()) { 
                    String nodes = entry.getValue()
                    .stream().map(Object::toString)
                    .collect(Collectors.joining(","));
                    compText.append("[").append(groupChar++).append(": ")
                    .append(nodes).append("] ");
                }

                System.out.println("\n ---------------------------");
                System.out.println("Paso "+ step + ":");
                System.out.println("Grupos actuales => " + compText);
                System.out.println("Siguiente arista => " + e);

                boolean wouldCycle = dsu.same(e.u,e.v); 
                System.out.println("¿Seleccionar o (n)o selecciona? ");
                String ans = sc.nextLine().trim().toLowerCase();
                while (!(ans.equals("s") || ans.equals("n"))) {
                    System.out.print("Por favor, escribe 's' o 'n':");
                    ans = sc.nextLine().trim().toLowerCase();
                }

                if(ans.equals("s")) { 
                    if(wouldCycle) { 
                        System.out.println("INCORRECTO => Esta arista forma un ciclo");
                        mistakes++;
                    } else { 
                        dsu.union(e.u, e.v);
                        chosen.add(e);
                        System.out.println("CORRECTO - Conecta dos grupos diferentes.");
                    }
                } else { 
                    if(wouldCycle) { 
                        System.out.println("CORRECTO => Rechazada porque formaba ciclo.");
                        System.out.println("INCORRECTO => No formaba ciclo, deberías aceptarla.");
                        mistakes++;
                    }
                }
                step++;
            }
            int total = chosen.stream().mapToInt(x -> x.w).sum();
            System.out.println("\n=========== RESULTADOS =============");
            System.out.println("Aristas seleccionadas.");
            for(Edge e : chosen) System.out.println("" + e);
            System.out.println("Peso total = " + total);
            System.out.println("Errores cometidos = " + mistakes);
            if(mistakes == 0) System.out.println("¡Excelente! Aplicaste correctamente Kruskal.");
            else System.out.println("Repasa cuándo se forma un  ciclo.");
            System.out.println("======================================");

        }
    }
}
