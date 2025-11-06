package com.lelyliliana;
 
import java.util.*;

/**
 * Floyd–Warshall con:
 *  - Reconstrucción de rutas (matriz next)
 *  - Detección de ciclos negativos
 *  - Impresión PASO A PASO por cada k (tablas)
 *
 * Entrada:
 *  N
 *  N líneas con N tokens (usa "INF" para infinito)
 *  showSteps (true/false)
 *  showNext (true/false)  -> si showSteps=true, permite ver también la matriz next en cada k
 *  Q
 *  Q líneas con u v para consultar ruta mínima
 */
public class FloydWarshallPasoAPaso {

    private static final long INF = (long) 1e15;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("== Algoritmo de Floyd–Warshall (con pasos) ==");
        System.out.print("Ingrese N (número de vértices): ");
        int N = readInt(sc);

        long[][] dist = new long[N][N];
        int[][] next = new int[N][N];

        System.out.println("Ingrese la matriz de adyacencia (" + N + " x " + N + "). Use 'INF' para infinito:");
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                dist[i][j] = parseWeight(sc.next());
            }
        }

        System.out.print("¿Mostrar pasos (tablas por k)? (true/false): ");
        boolean showSteps = Boolean.parseBoolean(sc.next());
        System.out.print("Si muestra pasos, ¿también mostrar la matriz 'next'? (true/false): ");
        boolean showNext = Boolean.parseBoolean(sc.next());

        // Inicialización de 'next'
        for (int i = 0; i < N; i++) {
            Arrays.fill(next[i], -1);
            for (int j = 0; j < N; j++) {
                if (i == j && dist[i][j] == 0) next[i][j] = j;
                else if (dist[i][j] < INF)    next[i][j] = j;
            }
        }

        // Estado inicial
        if (showSteps) {
            System.out.println("\n== Estado inicial (k = -1) ==");
            printDistMatrix(dist);
            if (showNext) printNextMatrix(next);
        }

        // Floyd–Warshall con impresión por k
        for (int k = 0; k < N; k++) {
            for (int i = 0; i < N; i++) {
                if (dist[i][k] == INF) continue;
                for (int j = 0; j < N; j++) {
                    if (dist[k][j] == INF) continue;
                    long throughK = dist[i][k] + dist[k][j];
                    if (throughK < dist[i][j]) {
                        dist[i][j] = throughK;
                        next[i][j] = next[i][k];
                    }
                }
            }
            if (showSteps) {
                System.out.println("\n== Después de considerar vértice intermedio k = " + k + " ==");
                printDistMatrix(dist);
                if (showNext) printNextMatrix(next);
            }
        }

        // Detección de ciclo negativo
        boolean hasNegCycle = false;
        for (int v = 0; v < N; v++) if (dist[v][v] < 0) { hasNegCycle = true; break; }

        System.out.println("\n== Matriz final de distancias mínimas ==");
        printDistMatrix(dist);
        if (hasNegCycle) {
            System.out.println("\n¡Atención! Se detectó al menos un ciclo negativo.");
        }

        System.out.print("\nIngrese Q (número de consultas u v): ");
        int Q = readInt(sc);
        for (int q = 1; q <= Q; q++) {
            System.out.print("Consulta " + q + " - ingrese u v: ");
            int u = readInt(sc), v = readInt(sc);

            if (dist[u][v] >= INF / 2) {
                System.out.println("No existe ruta de " + u + " a " + v + ".");
                continue;
            }
            List<Integer> path = reconstructPath(u, v, next);
            boolean touchesNeg = touchesNegativeCycle(path, dist);

            System.out.println("Distancia mínima de " + u + " a " + v + " = " + dist[u][v]);
            System.out.println("Ruta: " + path);
            if (touchesNeg) {
                System.out.println("Nota: La ruta podría estar afectada por un ciclo negativo.");
            }
        }
        System.out.println("\nFin.");
    }

    // ==== Utilidades ====
    private static int readInt(Scanner sc) {
        while (true) {
            String s = sc.next();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.print("Valor inválido. Intente de nuevo: "); }
        }
    }

    private static long parseWeight(String token) {
        if (token.equalsIgnoreCase("INF")) return INF;
        try { return Long.parseLong(token); }
        catch (NumberFormatException e) { return INF; }
    }

    private static List<Integer> reconstructPath(int u, int v, int[][] next) {
        List<Integer> path = new ArrayList<>();
        if (next[u][v] == -1) return path;
        int cur = u; path.add(cur);
        while (cur != v) {
            cur = next[cur][v];
            if (cur == -1) { path.clear(); return path; }
            path.add(cur);
        }
        return path;
    }

    private static boolean touchesNegativeCycle(List<Integer> path, long[][] dist) {
        for (int w : path) if (dist[w][w] < 0) return true;
        return false;
    }

    private static void printDistMatrix(long[][] dist) {
        int n = dist.length, width = 10;
        System.out.print(String.format("%" + width + "s", ""));
        for (int j = 0; j < n; j++) System.out.print(String.format("%" + width + "s", j));
        System.out.println();
        for (int i = 0; i < n; i++) {
            System.out.print(String.format("%" + width + "s", i));
            for (int j = 0; j < n; j++) {
                if (dist[i][j] >= INF / 2) System.out.print(String.format("%" + width + "s", "INF"));
                else                        System.out.print(String.format("%" + width + "d", dist[i][j]));
            }
            System.out.println();
        }
    }

    private static void printNextMatrix(int[][] next) {
        int n = next.length, width = 6;
        System.out.println("-- Matriz next (siguiente salto) --");
        System.out.print(String.format("%" + width + "s", ""));
        for (int j = 0; j < n; j++) System.out.print(String.format("%" + width + "s", j));
        System.out.println();
        for (int i = 0; i < n; i++) {
            System.out.print(String.format("%" + width + "s", i));
            for (int j = 0; j < n; j++) {
                String cell = (next[i][j] == -1) ? "-" : Integer.toString(next[i][j]);
                System.out.print(String.format("%" + width + "s", cell));
            }
            System.out.println();
        }
    }
}
