# 🌌 Raymarching Scene in GLSL

A minimal raymarching project written in **GLSL 330**, featuring procedurally generated 3D objects using **Signed Distance Functions (SDF)**.

The scene includes soft lighting, fog, animated shapes, and infinite object repetition using modular arithmetic.

---

## ✨ Features

- 🔷 **Raymarching** with distance-estimated SDFs
- 🔁 Infinite tiling in 3D space
- 💡 Diffuse + specular lighting
- 🌫 Exponential fog based on view distance
- 🎞 Time-based animation
- 🧊 Boolean operations: union, subtraction, smooth min
- 🔎 Normal calculation via central difference

---

## 📦 Scene Objects

- **Sphere**  
- **Box (as cutter)**  

All objects are repeated on a 3D grid using `mod()`.

---

## 💡 Shading

- Simple Blinn-Phong lighting
- Ambient + diffuse + specular
- Fog applied with:  
  `exp(-t * fogDensity)`
- Custom color blending for surface appearance

---

## 🧠 Techniques Used

- Distance functions for ray-object intersections
- Smooth boolean blending (`smin`, `sub`)
- Surface normals via tetrahedral sampling
- Animated geometry via time-based angle
- Procedural coloring and fog mixing

---

## 🔭 About

This project is a personal experiment in raymarching and real-time shader rendering using only GLSL. Inspired by works from [Inigo Quilez](https://iquilezles.org) and the ShaderToy community.

