"use client";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "../../lib/supabase";
import Link from "next/link";
export default function LoginPage() { const router=useRouter(); const [email,setEmail]=useState(""); const [password,setPassword]=useState(""); const [message,setMessage]=useState(""); async function submit(e:FormEvent){e.preventDefault();setMessage("");const {error}=await createClient().auth.signInWithPassword({email,password});if(error)setMessage(error.message);else router.push("/catalogo");} return <section className="card" style={{maxWidth:460,margin:"64px auto"}}><h1>Ingresar</h1><p className="muted">Accede al catálogo de tu equipo.</p><form onSubmit={submit} style={{display:"grid",gap:12}}><label>Correo<input type="email" required value={email} onChange={e=>setEmail(e.target.value)}/></label><label>Contraseña<input type="password" required minLength={8} value={password} onChange={e=>setPassword(e.target.value)}/></label><button>Ingresar</button></form><p><Link href="/registro">Crear una cuenta</Link></p>{message&&<p>{message}</p>}</section>; }
