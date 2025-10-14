package com.lelyliliana;

import java.util.*;

/**
 * Programa de consola para gestionar un Árbol Binario de Búsqueda (BST) de enteros.
 * Operaciones: crear (vaciar/cargar), insertar, eliminar, buscar y recorridos (inorden, preorden, postorden).
 * Autor: Leli :)
 */
public class ArbolBinarioMenu {

    // ======== NODO ========
    static class Nodo {
        int valor;
        Nodo izq, der;
        Nodo(int v) { this.valor = v; }
    }

    // ======== ÁRBOL BINARIO DE BÚSQUEDA ========
    static class ArbolBinarioBusqueda {
        private Nodo raiz;

        public void vaciar() { raiz = null; }

        public boolean estaVacio() { return raiz == null; }

        public boolean insertar(int v) {
            if (raiz == null) {
                raiz = new Nodo(v);
                return true;
            }
            Nodo actual = raiz, padre = null;
            while (actual != null) {
                padre = actual;
                if (v == actual.valor) return false; // no permitir duplicados
                if (v < actual.valor) actual = actual.izq;
                else actual = actual.der;
            }
            if (v < padre.valor) padre.izq = new Nodo(v);
            else padre.der = new Nodo(v);
            return true;
        }

        public boolean contiene(int v) {
            Nodo a = raiz;
            while (a != null) {
                if (v == a.valor) return true;
                a = (v < a.valor) ? a.izq : a.der;
            }
            return false;
        }

        public boolean eliminar(int v) {
            Nodo actual = raiz, padre = null;
            while (actual != null && actual.valor != v) {
                padre = actual;
                actual = (v < actual.valor) ? actual.izq : actual.der;
            }
            if (actual == null) return false; // no existe

            // Caso 1: nodo con 0 o 1 hijo
            if (actual.izq == null || actual.der == null) {
                Nodo hijo = (actual.izq != null) ? actual.izq : actual.der; // puede ser null
                if (padre == null) {
                    raiz = hijo; // eliminado era la raíz
                } else if (padre.izq == actual) {
                    padre.izq = hijo;
                } else {
                    padre.der = hijo;
                }
            } else {
                // Caso 2: nodo con 2 hijos -> reemplazar con sucesor inorden
                Nodo padreSuc = actual;
                Nodo suc = actual.der;
                while (suc.izq != null) {
                    padreSuc = suc;
                    suc = suc.izq;
                }
                // Copiar valor del sucesor y eliminar el sucesor
                actual.valor = suc.valor;
                if (padreSuc.izq == suc) padreSuc.izq = suc.der;
                else padreSuc.der = suc.der;
            }
            return true;
        }

        // ======== RECORRIDOS ========
        public List<Integer> inorden() {
            List<Integer> res = new ArrayList<>();
            inordenRec(raiz, res);
            return res;
        }
        private void inordenRec(Nodo n, List<Integer> out) {
            if (n == null) return;
            inordenRec(n.izq, out);
            out.add(n.valor);
            inordenRec(n.der, out);
        }

        public List<Integer> preorden() {
            List<Integer> res = new ArrayList<>();
            preordenRec(raiz, res);
            return res;
        }
        private void preordenRec(Nodo n, List<Integer> out) {
            if (n == null) return;
            out.add(n.valor);
            preordenRec(n.izq, out);
            preordenRec(n.der, out);
        }

        public List<Integer> postorden() {
            List<Integer> res = new ArrayList<>();
            postordenRec(raiz, res);
            return res;
        }
        private void postordenRec(Nodo n, List<Integer> out) {
            if (n == null) return;
            postordenRec(n.izq, out);
            postordenRec(n.der, out);
            out.add(n.valor);
        }

        // (Opcional) impresión por niveles para referencia rápida
        public String nivelesComoString() {
    if (raiz == null) return "(árbol vacío)";
    StringBuilder sb = new StringBuilder();
    Queue<Nodo> q = new ArrayDeque<>();
    q.add(raiz);

    while (!q.isEmpty()) {
        int tam = q.size();            // cantidad de nodos en este nivel
        for (int i = 0; i < tam; i++) {
            Nodo n = q.poll();
            sb.append(n.valor).append(" ");
            if (n.izq != null) q.add(n.izq);
            if (n.der != null) q.add(n.der);
        }
        sb.append("\n");
    }
    return sb.toString();
}
    }

    // ======== MENÚ ========
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        boolean salir = false;

        while (!salir) {
            System.out.println("\n===== MENÚ ÁRBOL BINARIO (BST) =====");
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
                    System.out.println("Árbol vacío. (Creado/Reseteado)");
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
                        System.out.println(ok ? "Insertado: " + v : "Ya existía (no se permiten duplicados).");
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

    private static void subMenuRecorridos(Scanner sc, ArbolBinarioBusqueda arbol) {
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
