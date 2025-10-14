package com.lelyliliana;

import java.util.*;

/**
 * Árbol AVL de enteros con menú de consola.
 * Operaciones: crear/vaciar, cargar lista, insertar, eliminar, buscar, recorridos y vista por niveles.
 * Mantiene balanceo automático mediante rotaciones.
 */
public class ArbolAVLMenu {

    // ======== NODO AVL ========
    static class Nodo {
        int valor;
        int altura;      // altura del nodo (hoja = 1)
        Nodo izq, der;
        Nodo(int v) { this.valor = v; this.altura = 1; }
    }

    // ======== ÁRBOL AVL ========
    static class ArbolAVL {
        private Nodo raiz;

        // --- utilidades de altura/balance ---
        private int altura(Nodo n) { return (n == null) ? 0 : n.altura; }
        private int balance(Nodo n) { return (n == null) ? 0 : altura(n.izq) - altura(n.der); }
        private void actualizarAltura(Nodo n) { n.altura = 1 + Math.max(altura(n.izq), altura(n.der)); }

        // --- rotaciones ---
        private Nodo rotacionDerecha(Nodo y) {
            Nodo x = y.izq;
            Nodo T2 = x.der;
            // rotar
            x.der = y;
            y.izq = T2;
            // actualizar alturas
            actualizarAltura(y);
            actualizarAltura(x);
            return x;
        }
        private Nodo rotacionIzquierda(Nodo x) {
            Nodo y = x.der;
            Nodo T2 = y.izq;
            // rotar
            y.izq = x;
            x.der = T2;
            // actualizar alturas
            actualizarAltura(x);
            actualizarAltura(y);
            return y;
        }

        // --- rebalancear un nodo ---
        private Nodo rebalancear(Nodo n) {
            actualizarAltura(n);
            int bf = balance(n);

            // Caso Izq-Izq
            if (bf > 1 && balance(n.izq) >= 0)
                return rotacionDerecha(n);

            // Caso Izq-Der
            if (bf > 1 && balance(n.izq) < 0) {
                n.izq = rotacionIzquierda(n.izq);
                return rotacionDerecha(n);
            }

            // Caso Der-Der
            if (bf < -1 && balance(n.der) <= 0)
                return rotacionIzquierda(n);

            // Caso Der-Izq
            if (bf < -1 && balance(n.der) > 0) {
                n.der = rotacionDerecha(n.der);
                return rotacionIzquierda(n);
            }

            return n; // ya balanceado
        }

        // --- API pública ---
        public void vaciar() { raiz = null; }
        public boolean estaVacio() { return raiz == null; }
        public boolean contiene(int v) { return contiene(raiz, v); }
        public boolean insertar(int v) {
            int antes = cantidadNodos();
            raiz = insertar(raiz, v);
            return cantidadNodos() > antes; // true si se insertó (no se permiten duplicados)
        }
        public boolean eliminar(int v) {
            int antes = cantidadNodos();
            raiz = eliminar(raiz, v);
            return cantidadNodos() < antes; // true si se eliminó
        }

        // --- búsqueda (recursiva) ---
        private boolean contiene(Nodo n, int v) {
            if (n == null) return false;
            if (v == n.valor) return true;
            return (v < n.valor) ? contiene(n.izq, v) : contiene(n.der, v);
        }

        // --- inserción (recursiva con rebalanceo) ---
        private Nodo insertar(Nodo n, int v) {
            if (n == null) return new Nodo(v);
            if (v == n.valor) return n; // ignorar duplicado
            if (v < n.valor) n.izq = insertar(n.izq, v);
            else n.der = insertar(n.der, v);
            return rebalancear(n);
        }

        // --- eliminación (recursiva con rebalanceo) ---
        private Nodo eliminar(Nodo n, int v) {
            if (n == null) return null;
            if (v < n.valor) n.izq = eliminar(n.izq, v);
            else if (v > n.valor) n.der = eliminar(n.der, v);
            else {
                // encontrado
                if (n.izq == null || n.der == null) {
                    n = (n.izq != null) ? n.izq : n.der; // puede ser null
                } else {
                    // sucesor inorden (mínimo del subárbol derecho)
                    Nodo suc = n.der;
                    while (suc.izq != null) suc = suc.izq;
                    n.valor = suc.valor;                 // copiar valor
                    n.der = eliminar(n.der, suc.valor);  // eliminar sucesor
                }
            }
            if (n == null) return null; // si quedó vacío
            return rebalancear(n);
        }

        // --- recorridos ---
        public List<Integer> inorden() { List<Integer> r = new ArrayList<>(); inorden(raiz, r); return r; }
        public List<Integer> preorden(){ List<Integer> r = new ArrayList<>(); preorden(raiz, r); return r; }
        public List<Integer> postorden(){ List<Integer> r = new ArrayList<>(); postorden(raiz, r); return r; }
        private void inorden(Nodo n, List<Integer> out){ if(n==null) return; inorden(n.izq,out); out.add(n.valor); inorden(n.der,out);}
        private void preorden(Nodo n, List<Integer> out){ if(n==null) return; out.add(n.valor); preorden(n.izq,out); preorden(n.der,out);}
        private void postorden(Nodo n, List<Integer> out){ if(n==null) return; postorden(n.izq,out); postorden(n.der,out); out.add(n.valor);}

        // --- utilitarios (opcional) ---
        public int cantidadNodos() { return contar(raiz); }
        private int contar(Nodo n){ return (n==null)?0:1+contar(n.izq)+contar(n.der); }
        public int altura(){ return altura(raiz); }

        // Vista por niveles (BFS) sin nulls
        public String nivelesComoString() {
            if (raiz == null) return "(árbol vacío)";
            StringBuilder sb = new StringBuilder();
            Queue<Nodo> q = new ArrayDeque<>();
            q.add(raiz);
            while (!q.isEmpty()) {
                int tam = q.size();
                for (int i = 0; i < tam; i++) {
                    Nodo n = q.poll();
                    sb.append(n.valor).append(" ");
                    if (n.izq != null) q.add(n.izq);
                    if (n.der != null) q.add(n.der);
                }
                sb.append("\n");
            }
            sb.append("(altura AVL = ").append(altura()).append(")\n");
            return sb.toString();
        }
    }

    // ======== MENÚ ========
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArbolAVL arbol = new ArbolAVL();
        boolean salir = false;

        while (!salir) {
            System.out.println("\n===== MENÚ ÁRBOL AVL =====");
            System.out.println("1. Crear/Vaciar árbol");
            System.out.println("2. Cargar una lista de valores (ej: 8,3,10,1,6,14,4,7,13)");
            System.out.println("3. Insertar un valor");
            System.out.println("4. Eliminar un valor");
            System.out.println("5. Buscar un valor");
            System.out.println("6. Recorridos");
            System.out.println("7. Ver árbol por niveles");
            System.out.println("0. Salir");
            System.out.print("Elige una opción: ");
            String op = sc.nextLine().trim();

            switch (op) {
                case "1":
                    arbol.vaciar();
                    System.out.println("Árbol vaciado.");
                    break;
                case "2":
                    System.out.print("Ingresa la lista (separada por comas): ");
                    String linea = sc.nextLine();
                    String[] partes = linea.split(",");
                    int insOk = 0, insDup = 0;
                    for (String p : partes) {
                        String t = p.trim();
                        if (t.isEmpty()) continue;
                        try {
                            int v = Integer.parseInt(t);
                            if (arbol.insertar(v)) insOk++; else insDup++;
                        } catch (NumberFormatException e) {
                            System.out.println("Valor ignorado (no entero): " + t);
                        }
                    }
                    System.out.println("Insertados: " + insOk + " | Duplicados ignorados: " + insDup);
                    break;
                case "3":
                    System.out.print("Valor a insertar: ");
                    try {
                        int v = Integer.parseInt(sc.nextLine().trim());
                        boolean ok = arbol.insertar(v);
                        System.out.println(ok ? "Insertado: " + v : "Duplicado (ignorado).");
                    } catch (NumberFormatException e) {
                        System.out.println("Debes ingresar un entero.");
                    }
                    break;
                case "4":
                    System.out.print("Valor a eliminar: ");
                    try {
                        int v = Integer.parseInt(sc.nextLine().trim());
                        boolean ok = arbol.eliminar(v);
                        System.out.println(ok ? "Eliminado: " + v : "No se encontró el valor.");
                    } catch (NumberFormatException e) {
                        System.out.println("Debes ingresar un entero.");
                    }
                    break;
                case "5":
                    System.out.print("Valor a buscar: ");
                    try {
                        int v = Integer.parseInt(sc.nextLine().trim());
                        System.out.println(arbol.contiene(v) ? "Sí está en el árbol." : "No está en el árbol.");
                    } catch (NumberFormatException e) {
                        System.out.println("Debes ingresar un entero.");
                    }
                    break;
                case "6":
                    subMenuRecorridos(sc, arbol);
                    break;
                case "7":
                    System.out.println(arbol.nivelesComoString());
                    break;
                case "0":
                    salir = true;
                    break;
                default:
                    System.out.println("Opción inválida.");
            }
        }

        System.out.println("¡Hasta luego!");
        sc.close();
    }

    private static void subMenuRecorridos(Scanner sc, ArbolAVL arbol) {
        if (arbol.estaVacio()) {
            System.out.println("El árbol está vacío. Inserta elementos primero.");
            return;
        }
        System.out.println("\n--- RECORRIDOS ---");
        System.out.println("1) Inorden   (Izq, Raíz, Der)");
        System.out.println("2) Preorden  (Raíz, Izq, Der)");
        System.out.println("3) Postorden (Izq, Der, Raíz)");
        System.out.print("Elige: ");
        String r = sc.nextLine().trim();
        List<Integer> res;
        switch (r) {
            case "1":
                res = arbol.inorden();
                System.out.println("Inorden : " + res);
                break;
            case "2":
                res = arbol.preorden();
                System.out.println("Preorden: " + res);
                break;
            case "3":
                res = arbol.postorden();
                System.out.println("Postorden: " + res);
                break;
            default:
                System.out.println("Opción inválida.");
        }
    }
}