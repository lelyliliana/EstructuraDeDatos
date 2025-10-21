package com.lelyliliana;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Explorador Multimedia con B+ y Hash
 * -----------------------------------
 * Unidad 1 – Árboles B, B+ y Hashing
 *
 * Qué permite:
 *  - Registrar ítems multimedia (canciones/películas/series)
 *  - Índices B+ por título, artista, género y región (exacto/prefijo + listado ordenado)
 *  - Índice Hash adicional por TÍTULO (búsqueda exacta O(1) promedio, ideal para comparar)
 *  - Eliminar registros (sincroniza ambos índices)
 *  - Mostrar estructura B+ (niveles)
 *  - Guardar/Cargar colección (reindexa ambos al cargar)
 *
 * Nota didáctica:
 *  - B+: mantiene orden y soporta prefijos eficientemente (scan por hojas).
 *  - Hash: exacto muy rápido; no mantiene orden ni hace prefijos sin escaneo.
 */
public class ExplorerApp {

    // ===================== Dominio =====================
    static class MediaItem implements Serializable {
        final int id;              // clave primaria
        String titulo;
        String artista;
        String genero;
        String region;
        Integer anio;              // opcional

        MediaItem(int id, String titulo, String artista, String genero, String region, Integer anio) {
            this.id = id;
            this.titulo = titulo;
            this.artista = artista;
            this.genero = genero;
            this.region = region;
            this.anio = anio;
        }
        @Override public String toString() {
            return String.format("[%d] %s — %s | %s | %s%s", id,
                    titulo, nullToDash(artista), nullToDash(genero), nullToDash(region),
                    anio != null ? (" | " + anio) : "");
        }
        private static String nullToDash(String s){ return (s==null||s.isBlank())?"-":s; }
    }

    // ===================== B+ Tree (genérico K -> List<V>) =====================
    static class BPlusTree<K extends Comparable<K>, V> {
        private final int ORDER; // máximo de claves por nodo
        private Node<K,V> root;
        private LeafNode<K,V> firstLeaf; // para recorrido ordenado

        abstract static class Node<K extends Comparable<K>, V> {
            List<K> keys = new ArrayList<>();
            abstract boolean isLeaf();
        }
        static class InternalNode<K extends Comparable<K>, V> extends Node<K,V> {
            List<Node<K,V>> children = new ArrayList<>();
            @Override boolean isLeaf() { return false; }
        }
        static class LeafNode<K extends Comparable<K>, V> extends Node<K,V> {
            List<List<V>> values = new ArrayList<>();
            LeafNode<K,V> next; // para scans
            @Override boolean isLeaf() { return true; }
        }

        public BPlusTree(int order) {
            if (order < 3) throw new IllegalArgumentException("ORDER debe ser >= 3");
            this.ORDER = order;
            this.root = new LeafNode<>();
            this.firstLeaf = (LeafNode<K,V>) root;
        }

        private int lowerBound(List<K> keys, K key) {
            int lo = 0, hi = keys.size();
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (keys.get(mid).compareTo(key) < 0) lo = mid + 1; else hi = mid;
            }
            return lo;
        }

        private LeafNode<K,V> findLeaf(K key) {
            Node<K,V> n = root;
            while (!n.isLeaf()) {
                InternalNode<K,V> in = (InternalNode<K,V>) n;
                int idx = lowerBound(in.keys, key);
                n = in.children.get(idx);
            }
            return (LeafNode<K,V>) n;
        }
        public List<V> searchExact(K key) {
            LeafNode<K,V> leaf = findLeaf(key);
            int i = lowerBound(leaf.keys, key);
            if (i < leaf.keys.size() && leaf.keys.get(i).compareTo(key) == 0) {
                return new ArrayList<>(leaf.values.get(i));
            }
            return Collections.emptyList();
        }
        public List<V> searchPrefix(K fromInclusive, java.util.function.Predicate<K> stillMatches) {
            List<V> out = new ArrayList<>();
            LeafNode<K,V> leaf = findLeaf(fromInclusive);
            int i = lowerBound(leaf.keys, fromInclusive);
            for (LeafNode<K,V> cur = leaf; cur != null; cur = cur.next) {
                for (int j = (cur==leaf? i:0); j < cur.keys.size(); j++) {
                    K k = cur.keys.get(j);
                    if (!stillMatches.test(k)) return out;
                    out.addAll(cur.values.get(j));
                }
            }
            return out;
        }

        public void insert(K key, V value) {
            SplitResult<K,V> split = insertRecursive(root, key, value);
            if (split != null) {
                InternalNode<K,V> newRoot = new InternalNode<>();
                newRoot.keys.add(split.pivot);
                newRoot.children.add(split.left);
                newRoot.children.add(split.right);
                root = newRoot;
            }
        }
        private static class SplitResult<K extends Comparable<K>, V> {
            K pivot; Node<K,V> left, right;
            SplitResult(K p, Node<K,V> l, Node<K,V> r){ pivot=p; left=l; right=r; }
        }
        private SplitResult<K,V> insertRecursive(Node<K,V> node, K key, V value) {
            if (node.isLeaf()) {
                LeafNode<K,V> leaf = (LeafNode<K,V>) node;
                int pos = lowerBound(leaf.keys, key);
                if (pos < leaf.keys.size() && leaf.keys.get(pos).compareTo(key)==0) {
                    leaf.values.get(pos).add(value);
                } else {
                    leaf.keys.add(pos, key);
                    List<V> list = new ArrayList<>(); list.add(value);
                    leaf.values.add(pos, list);
                }
                if (leaf.keys.size() > ORDER) {
                    return splitLeaf(leaf);
                }
                return null;
            } else {
                InternalNode<K,V> in = (InternalNode<K,V>) node;
                int idx = lowerBound(in.keys, key);
                SplitResult<K,V> childSplit = insertRecursive(in.children.get(idx), key, value);
                if (childSplit != null) {
                    in.keys.add(idx, childSplit.pivot);
                    in.children.set(idx, childSplit.left);
                    in.children.add(idx+1, childSplit.right);
                    if (in.keys.size() > ORDER) {
                        return splitInternal(in);
                    }
                }
                return null;
            }
        }
        private SplitResult<K,V> splitLeaf(LeafNode<K,V> leaf) {
            int mid = leaf.keys.size()/2;
            LeafNode<K,V> right = new LeafNode<>();
            right.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
            right.values.addAll(leaf.values.subList(mid, leaf.values.size()));

            leaf.keys.subList(mid, leaf.keys.size()).clear();
            leaf.values.subList(mid, leaf.values.size()).clear();

            right.next = leaf.next; leaf.next = right;
            if (firstLeaf == leaf && leaf.keys.isEmpty()) firstLeaf = right;

            K pivot = right.keys.get(0);
            return new SplitResult<>(pivot, leaf, right);
        }
        private SplitResult<K,V> splitInternal(InternalNode<K,V> in) {
            int mid = in.keys.size()/2;
            K pivot = in.keys.get(mid);

            InternalNode<K,V> right = new InternalNode<>();
            right.keys.addAll(in.keys.subList(mid+1, in.keys.size()));
            right.children.addAll(in.children.subList(mid+1, in.children.size()));

            in.keys = new ArrayList<>(in.keys.subList(0, mid));
            in.children = new ArrayList<>(in.children.subList(0, mid+1));

            return new SplitResult<>(pivot, in, right);
        }

        // Eliminación simple (no merge/borrow, suficiente para demo)
        public boolean delete(K key, java.util.function.Predicate<V> valueMatch) {
            return deleteRecursive(root, key, valueMatch);
        }
        private boolean deleteRecursive(Node<K,V> node, K key, java.util.function.Predicate<V> valueMatch) {
            if (node.isLeaf()) {
                LeafNode<K,V> leaf = (LeafNode<K,V>) node;
                int pos = lowerBound(leaf.keys, key);
                if (pos < leaf.keys.size() && leaf.keys.get(pos).compareTo(key)==0) {
                    List<V> list = leaf.values.get(pos);
                    boolean removed = list.removeIf(valueMatch);
                    if (list.isEmpty()) {
                        leaf.keys.remove(pos);
                        leaf.values.remove(pos);
                    }
                    return removed;
                }
                return false;
            } else {
                InternalNode<K,V> in = (InternalNode<K,V>) node;
                int idx = lowerBound(in.keys, key);
                return deleteRecursive(in.children.get(idx), key, valueMatch);
            }
        }

        public Iterable<Map.Entry<K,List<V>>> scanAll() {
            return () -> new Iterator<>() {
                LeafNode<K,V> cur = firstLeaf;
                int i = 0;
                @Override public boolean hasNext() {
                    while (cur != null && i >= cur.keys.size()) { cur = cur.next; i = 0; }
                    return cur != null && i < cur.keys.size();
                }
                @Override public Map.Entry<K, List<V>> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    Map.Entry<K,List<V>> e = new AbstractMap.SimpleEntry<>(cur.keys.get(i), cur.values.get(i));
                    i++; return e;
                }
            };
        }

        public void printLevels() {
            Queue<Node<K,V>> q = new ArrayDeque<>();
            q.add(root);
            int level = 0;
            while (!q.isEmpty()) {
                int sz = q.size();
                System.out.printf("Nivel %d: ", level++);
                for (int i=0;i<sz;i++) {
                    Node<K,V> n = q.poll();
                    if (n.isLeaf()) {
                        LeafNode<K,V> lf = (LeafNode<K,V>) n;
                        System.out.print(lf.keys + " ");
                    } else {
                        InternalNode<K,V> in = (InternalNode<K,V>) n;
                        System.out.print(in.keys + " ");
                        q.addAll(in.children);
                    }
                }
                System.out.println();
            }
        }
    }

    // ===================== Índice Hash (encadenamiento por clave String) =====================
    static class HashIndex {
        private final Map<String, List<Integer>> map = new HashMap<>();
        private static String norm(String s){ return s==null? "" : s.trim().toLowerCase(Locale.ROOT); }

        public void clear(){ map.clear(); }

        public void insert(String key, int id){
            key = norm(key);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(id);
        }
        public boolean delete(String key, int id){
            key = norm(key);
            var list = map.get(key);
            if (list == null) return false;
            boolean removed = list.removeIf(v -> v == id);
            if (list.isEmpty()) map.remove(key);
            return removed;
        }
        public List<Integer> searchExact(String key){
            key = norm(key);
            var list = map.get(key);
            return list == null ? List.of() : new ArrayList<>(list);
        }
        // Prefijo con hash (no ideal): escanea claves
        public List<Integer> searchPrefix(String prefix){
            prefix = norm(prefix);
            List<Integer> out = new ArrayList<>();
            for (var e : map.entrySet()){
                if (e.getKey().startsWith(prefix)) out.addAll(e.getValue());
            }
            return out;
        }
        // Para comparar listados: ordenar claves y concatenar
        public List<Integer> listAllOrdered(){
            List<String> keys = new ArrayList<>(map.keySet());
            keys.sort(String::compareTo);
            List<Integer> out = new ArrayList<>();
            for (String k : keys) out.addAll(map.get(k));
            return out;
        }
    }

    // ===================== Repositorio con B+ (4 índices) + Hash (por TÍTULO) =====================
    static class MediaRepository {
        private final Map<Integer, MediaItem> store = new HashMap<>();

        // B+ por campo
        private BPlusTree<String,Integer> byTitle;
        private BPlusTree<String,Integer> byArtist;
        private BPlusTree<String,Integer> byGenre;
        private BPlusTree<String,Integer> byRegion;

        // Hash adicional SOLO por título (comparación)
        private final HashIndex titleHash = new HashIndex();

        private int nextId = 1;

        MediaRepository(){ resetIndexes(); }

        private void resetIndexes(){
            byTitle = new BPlusTree<>(4);
            byArtist = new BPlusTree<>(4);
            byGenre = new BPlusTree<>(4);
            byRegion = new BPlusTree<>(4);
            titleHash.clear();
        }

        public void clearAndReindex(Collection<MediaItem> items){
            store.clear();
            resetIndexes();
            nextId = 1;
            int maxId = 0;
            for (MediaItem m: items){
                store.put(m.id, m);
                indexInsert(m);
                if (m.id > maxId) maxId = m.id;
            }
            nextId = maxId + 1;
        }

        private static String norm(String s){ return s==null?"":s.trim().toLowerCase(Locale.ROOT); }

        private void indexInsert(MediaItem m){
            byTitle.insert(norm(m.titulo), m.id);
            byArtist.insert(norm(m.artista), m.id);
            byGenre.insert(norm(m.genero), m.id);
            byRegion.insert(norm(m.region), m.id);
            titleHash.insert(m.titulo, m.id); // hash sin normalizar internamente normaliza
        }

        private void indexDelete(MediaItem m){
            String t=norm(m.titulo), a=norm(m.artista), g=norm(m.genero), r=norm(m.region);
            byTitle.delete(t, v-> Objects.equals(v, m.id));
            byArtist.delete(a, v-> Objects.equals(v, m.id));
            byGenre.delete(g, v-> Objects.equals(v, m.id));
            byRegion.delete(r, v-> Objects.equals(v, m.id));
            titleHash.delete(m.titulo, m.id);
        }

        public MediaItem add(String titulo, String artista, String genero, String region, Integer anio){
            int id = nextId++;
            MediaItem m = new MediaItem(id, titulo, artista, genero, region, anio);
            store.put(id, m);
            indexInsert(m);
            return m;
        }

        public boolean remove(int id){
            MediaItem m = store.remove(id);
            if (m==null) return false;
            indexDelete(m);
            return true;
        }

        // ---------- Consultas B+ ----------
        public List<MediaItem> searchExactBPlus(String field, String value){
            String k = norm(value);
            List<Integer> ids = switch (field){
                case "titulo" -> byTitle.searchExact(k);
                case "artista" -> byArtist.searchExact(k);
                case "genero" -> byGenre.searchExact(k);
                case "region" -> byRegion.searchExact(k);
                default -> List.of();
            };
            return idsToItems(ids);
        }
        public List<MediaItem> searchPrefixBPlus(String field, String prefix){
            String p = norm(prefix);
            var pred = (java.util.function.Predicate<String>) (s -> s.startsWith(p));
            List<Integer> ids = switch (field){
                case "titulo" -> byTitle.searchPrefix(p, pred);
                case "artista" -> byArtist.searchPrefix(p, pred);
                case "genero" -> byGenre.searchPrefix(p, pred);
                case "region" -> byRegion.searchPrefix(p, pred);
                default -> List.of();
            };
            return idsToItems(ids);
        }
        public List<MediaItem> listAllOrderedByTitleBPlus(){
            List<MediaItem> out = new ArrayList<>();
            for (var e : byTitle.scanAll()){
                for (Integer id: e.getValue()){
                    MediaItem m = store.get(id);
                    if (m!=null) out.add(m);
                }
            }
            return out;
        }

        // ---------- Consultas Hash (solo título) ----------
        public List<MediaItem> searchExactTitleHash(String title){
            List<Integer> ids = titleHash.searchExact(title);
            return idsToItems(ids);
        }
        public List<MediaItem> searchPrefixTitleHash(String prefix){
            List<Integer> ids = titleHash.searchPrefix(prefix);
            return idsToItems(ids);
        }
        public List<MediaItem> listAllOrderedByTitleHash(){
            // No hay orden interno; ordenamos por clave después
            List<Integer> ids = titleHash.listAllOrdered();
            return idsToItems(ids);
        }

        private List<MediaItem> idsToItems(List<Integer> ids){
            List<MediaItem> out = new ArrayList<>();
            for (Integer id: ids) {
                MediaItem m = store.get(id);
                if (m!=null) out.add(m);
            }
            // orden estable por título para consistencia visual
            out.sort(Comparator.comparing(mi -> mi.titulo.toLowerCase(Locale.ROOT)));
            return out;
        }

        public void printIndexStructure(){
            System.out.println("== Índice B+ por TÍTULO ==");
            byTitle.printLevels();
            System.out.println("== Índice B+ por ARTISTA ==");
            byArtist.printLevels();
            System.out.println("== Índice B+ por GÉNERO ==");
            byGenre.printLevels();
            System.out.println("== Índice B+ por REGIÓN ==");
            byRegion.printLevels();
        }

        // Dataset centrado en música latinoamericana (énfasis Colombia)
        public void loadSampleDataLatam(){
            add("Cali Pachanguero", "Grupo Niche", "Salsa Colombiana", "CO", 1984);
            add("La Gota Fría", "Carlos Vives", "Vallenato/Pop", "CO", 1993);
            add("En Barranquilla Me Quedo", "Joe Arroyo", "Salsa/Son", "CO", 1988);
            add("El Pescador", "Totó la Momposina", "Cumbia/Folclore", "CO", 1993);
            add("Yo Me Llamo Cumbia", "Mario Gareña", "Cumbia", "CO", 1969);
            add("Mujer de Mil Batallas", "Peter Manjarrés", "Vallenato Romántico", "CO", 2008);
            add("Salió El Sol", "Silvestre Dangond", "Vallenato", "CO", 2005);
            add("El Preso", "Fruko y sus Tesos", "Salsa Colombiana", "CO", 1975);
            add("Campesina Santandereana", "Garzón y Collazos", "Bambuco/Andina", "CO", 1950);
            add("Ay Mi Llanura", "Arnulfo Briceño", "Joropo/Llanera", "CO", 1962);
            add("La Tierra del Olvido", "Carlos Vives", "Vallenato/Fusión", "CO", 1995);
            add("La Rebelión", "Joe Arroyo", "Salsa", "CO", 1986);
            add("La Piragua", "José Barros", "Cumbia Tradicional", "CO", 1960);
            add("La Colegiala", "Rodolfo Aicardi", "Cumbia", "CO", 1976);
            add("Latinoamérica", "Calle 13", "Fusión/Protesta", "PR", 2011);
            add("Rayando el Sol", "Maná", "Rock Latino", "MX", 1990);
            add("Oye Mi Amor", "Maná", "Rock Latino", "MX", 1992);
            add("Lamento Boliviano", "Enanitos Verdes", "Rock en Español", "AR", 1994);
            add("La Camisa Negra", "Juanes", "Pop/Rock Latino", "CO", 2005);
            add("La Luz", "Juanes", "Pop Latino", "CO", 2014);
        }
    }

    // ===================== Persistencia sencilla (lista de MediaItem) =====================
    static class Persistence {
        static void save(MediaRepository repo, String path) {
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(Path.of(path)))){
                oos.writeObject(new ArrayList<>(repo.store.values()));
            } catch (IOException e) { System.err.println("No se pudo guardar: "+e.getMessage()); }
        }
        static List<MediaItem> load(String path){
            if (!Files.exists(Path.of(path))) return List.of();
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(Path.of(path)))){
                @SuppressWarnings("unchecked")
                List<MediaItem> list = (List<MediaItem>) ois.readObject();
                return list;
            } catch (Exception e){ System.err.println("No se pudo cargar: "+e.getMessage()); return List.of(); }
        }
    }

    // ===================== CLI =====================
    private final MediaRepository repo = new MediaRepository();
    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        new ExplorerApp().run();
    }
    private void run(){
        System.out.println("Explorador Multimedia (B+ y Hash)");
        System.out.println("Cargando dataset latino de ejemplo...");
        repo.loadSampleDataLatam();
        loop();
    }
    private void loop(){
        while(true){
            System.out.println("\n=== MENÚ ===");
            System.out.println("1) Registrar nuevo");
            System.out.println("2) Buscar (B+)  [exacto/prefijo en titulo|artista|genero|region]");
            System.out.println("3) Buscar por TÍTULO (Hash)  [exacto/prefijo]");
            System.out.println("4) Listar en orden por TÍTULO (B+)");
            System.out.println("5) Mostrar estructura B+ (niveles)");
            System.out.println("6) Guardar/Cargar (simple)");
            System.out.println("7) Comparar tiempos: B+ vs Hash (búsqueda exacta por título)");
            System.out.println("0) Salir");
            System.out.print("Opción: ");
            String op = sc.nextLine().trim();
            switch (op){
                case "1" -> registrar();
                case "2" -> buscarBPlus();
                case "3" -> buscarHash();
                case "4" -> listarBPlus();
                case "5" -> repo.printIndexStructure();
                case "6" -> guardarCargar();
                case "7" -> compararTiempos();
                case "0" -> { System.out.println("¡Hasta luego!"); return; }
                default -> System.out.println("Opción no válida");
            }
        }
    }

    private void registrar(){
        System.out.println("— Registrar ítem —");
        System.out.print("Título: "); String t = sc.nextLine();
        System.out.print("Artista/Autor: "); String a = sc.nextLine();
        System.out.print("Género: "); String g = sc.nextLine();
        System.out.print("Región: "); String r = sc.nextLine();
        System.out.print("Año (opcional): "); String an = sc.nextLine();
        Integer y = an.isBlank()? null : parseIntSafe(an);
        var m = repo.add(t,a,g,r,y);
        System.out.println("Guardado: "+m);
    }

    private void buscarBPlus(){
        System.out.println("— Buscar (B+) — campos: titulo | artista | genero | region");
        System.out.print("Campo: "); String f = sc.nextLine().trim().toLowerCase(Locale.ROOT);
        System.out.print("Texto de búsqueda: "); String q = sc.nextLine();
        System.out.print("¿Prefijo? (s/n): "); boolean pref = sc.nextLine().trim().equalsIgnoreCase("s");

        long t0 = System.nanoTime();
        List<MediaItem> res = pref ? repo.searchPrefixBPlus(f,q) : repo.searchExactBPlus(f,q);
        long t1 = System.nanoTime();

        mostrarResultados(res);
        System.out.printf("Tiempo B+: %.3f ms%n", (t1 - t0)/1e6);
    }

    private void buscarHash(){
        System.out.println("— Buscar por TÍTULO (Hash) —");
        System.out.print("Texto de búsqueda: "); String q = sc.nextLine();
        System.out.print("¿Prefijo? (s/n): "); boolean pref = sc.nextLine().trim().equalsIgnoreCase("s");

        long t0 = System.nanoTime();
        List<MediaItem> res = pref ? repo.searchPrefixTitleHash(q) : repo.searchExactTitleHash(q);
        long t1 = System.nanoTime();

        mostrarResultados(res);
        System.out.printf("Tiempo Hash: %.3f ms%n", (t1 - t0)/1e6);
    }

    private void listarBPlus(){
        System.out.println("— Listado ordenado por TÍTULO (B+) —");
        long t0 = System.nanoTime();
        var list = repo.listAllOrderedByTitleBPlus();
        long t1 = System.nanoTime();
        mostrarResultados(list);
        System.out.printf("Tiempo (scan hojas B+): %.3f ms%n", (t1 - t0)/1e6);
    }

    private void guardarCargar(){
        System.out.println("1) Guardar  2) Cargar");
        String op = sc.nextLine().trim();
        String path = "media_store.bin";
        if ("1".equals(op)){
            Persistence.save(repo, path);
            System.out.println("Guardado a "+path);
        } else if ("2".equals(op)){
            List<MediaItem> list = Persistence.load(path);
            repo.clearAndReindex(list);
            System.out.println("Cargados "+list.size()+" elementos (reindexados B+ & Hash).");
        }
    }

    private void compararTiempos(){
        System.out.println("— Comparar tiempos (búsqueda exacta por TÍTULO) —");
        System.out.print("Título exacto a buscar: ");
        String q = sc.nextLine();

        long t0 = System.nanoTime();
        var rB = repo.searchExactBPlus("titulo", q);
        long t1 = System.nanoTime();
        var rH = repo.searchExactTitleHash(q);
        long t2 = System.nanoTime();

        System.out.printf("Resultados B+: %d  | Tiempo: %.3f ms%n", rB.size(), (t1 - t0)/1e6);
        System.out.printf("Resultados Hash: %d | Tiempo: %.3f ms%n", rH.size(), (t2 - t1)/1e6);
        if (!rB.equals(rH)) {
            System.out.println("⚠ Diferencia en resultados (revisa normalización/acentos).");
        }
    }

    private static void mostrarResultados(List<MediaItem> res){
        if (res.isEmpty()){ System.out.println("(sin resultados)"); return; }
        for (MediaItem m: res) System.out.println(" • "+m);
        System.out.println("Total: "+res.size());
    }
    private static Integer parseIntSafe(String s){
        try { return Integer.parseInt(s.trim()); } catch (Exception e){ return null; }
    }
}
