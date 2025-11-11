import streamlit as st
import numpy as np
import face_recognition
from PIL import Image, ImageDraw, ImageFont
import io, os, pickle

DB_PATH = "encodings.pkl"

# ---------- Utilidades ----------
def load_db():
    if os.path.exists(DB_PATH):
        with open(DB_PATH, "rb") as f:
            return pickle.load(f)  # dict: {"names": [], "encodings": [np.array,...]}
    return {"names": [], "encodings": []}

def save_db(db):
    with open(DB_PATH, "wb") as f:
        pickle.dump(db, f)

def image_to_array(img: Image.Image):
    # face_recognition espera RGB ndarray
    return np.array(img.convert("RGB"))

def draw_boxes(img: Image.Image, locations, labels=None):
    draw = ImageDraw.Draw(img)
    for i, (top, right, bottom, left) in enumerate(locations):
        draw.rectangle(((left, top), (right, bottom)), outline=(0, 255, 0), width=3)
        if labels and i < len(labels):
            text = labels[i]
            # Caja de texto simple
            text_w, text_h = draw.textlength(text), 16
            draw.rectangle(((left, bottom), (left + text_w + 6, bottom + text_h + 6)), fill=(0, 255, 0))
            draw.text((left + 3, bottom + 3), text, fill=(0, 0, 0))
    return img

@st.cache_data(show_spinner=False)
def compute_face_encodings(image_array, model="small"):
    # model: "small" (hog) o "large" (cnn – requiere GPU/CUDA)
    # Detectar ubicaciones
    locations = face_recognition.face_locations(image_array, model=model)
    encs = face_recognition.face_encodings(image_array, known_face_locations=locations)
    return locations, encs

# ---------- UI ----------
st.set_page_config(page_title="Demo Reconocimiento Facial", page_icon="🧠", layout="centered")
st.title("🧠 Reconocimiento Facial (demo local)")

tabs = st.tabs(["➕ Registrar rostro", "🔎 Reconocer", "⚙️ Administración"])

with tabs[0]:
    st.subheader("Registrar un nuevo rostro")
    nombre = st.text_input("Nombre de la persona (como quieres guardarlo):")
    foto = st.file_uploader("Sube una foto nítida del rostro (formato JPG/PNG)", type=["jpg", "jpeg", "png"])
    modelo = st.radio("Modelo de detección", ["small (HOG - CPU)", "large (CNN - GPU)"], index=0, horizontal=True)
    tol = st.slider("Tolerancia de coincidencia (más bajo = más estricto)", 0.3, 0.7, 0.45, 0.01)

    if st.button("Registrar"):
        if not nombre.strip():
            st.warning("Ingresa un nombre.")
        elif not foto:
            st.warning("Sube una imagen.")
        else:
            img = Image.open(io.BytesIO(foto.read()))
            arr = image_to_array(img)
            detect_model = "small" if "small" in modelo else "large"
            locations, encs = compute_face_encodings(arr, detect_model)
            if len(encs) == 0:
                st.error("No se detectó ningún rostro. Prueba con otra imagen (frontal, bien iluminada).")
            elif len(encs) > 1:
                st.error("Se detectaron varios rostros. Sube una foto con un único rostro.")
                boxed = draw_boxes(img.copy(), locations, ["?"]*len(locations))
                st.image(boxed, caption="Rostros detectados", use_column_width=True)
            else:
                db = load_db()
                db["names"].append(nombre.strip())
                db["encodings"].append(encs[0])
                save_db(db)
                boxed = draw_boxes(img.copy(), locations, [nombre.strip()])
                st.success(f"✅ Rostro de '{nombre.strip()}' registrado.")
                st.image(boxed, caption="Rostro registrado", use_container_width=True)

with tabs[1]:
    st.subheader("Reconocer rostro en una imagen")
    foto_test = st.file_uploader("Sube la foto a reconocer (JPG/PNG)", type=["jpg", "jpeg", "png"], key="rec")
    modelo2 = st.radio("Modelo de detección", ["small (HOG - CPU)", "large (CNN - GPU)"], index=0, horizontal=True, key="mod2")
    tol2 = st.slider("Tolerancia (recomendado 0.45)", 0.3, 0.7, 0.45, 0.01, key="tol2")

    if st.button("Reconocer"):
        if not foto_test:
            st.warning("Sube una imagen.")
        else:
            db = load_db()
            if len(db["encodings"]) == 0:
                st.info("La base está vacía. Primero registra al menos un rostro.")
            else:
                img = Image.open(io.BytesIO(foto_test.read()))
                arr = image_to_array(img)
                detect_model = "small" if "small" in modelo2 else "large"
                locations, encs = compute_face_encodings(arr, detect_model)

                if len(encs) == 0:
                    st.error("No se detectaron rostros en la imagen.")
                else:
                    labels = []
                    for enc in encs:
                        # Distancias a todos los registrados
                        dists = face_recognition.face_distance(db["encodings"], enc)
                        if len(dists) == 0:
                            labels.append("Desconocido")
                            continue
                        min_idx = int(np.argmin(dists))
                        min_dist = float(dists[min_idx])
                        match = min_dist <= tol2
                        label = f"{db['names'][min_idx]} (dist {min_dist:.3f})" if match else f"Desconocido (dist {min_dist:.3f})"
                        labels.append(label)

                    img_out = draw_boxes(img.copy(), locations, labels)
                    st.image(img_out, caption="Resultado", use_container_width=True)
                    # Resumen tabular
                    st.write("**Coincidencias encontradas:**")
                    for lab in labels:
                        st.write("- ", lab)

with tabs[2]:
    st.subheader("Administración de datos")
    db = load_db()
    st.write(f"👥 Personas registradas: **{len(db['names'])}**")
    if len(db["names"]) > 0:
        st.write(", ".join(sorted(set(db["names"]))))

    col1, col2 = st.columns(2)
    with col1:
        if st.button("🗑️ Vaciar base (encodings.pkl)"):
            if os.path.exists(DB_PATH):
                os.remove(DB_PATH)
            st.success("Base borrada.")
    with col2:
        st.download_button("⬇️ Descargar base", data=open(DB_PATH, "rb").read() if os.path.exists(DB_PATH) else b"", file_name="encodings.pkl", mime="application/octet-stream")
