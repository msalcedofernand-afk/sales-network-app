# Skill: Versionado de aplicaciones

## Objetivo

Esta Skill define cómo administrar automáticamente las versiones de una aplicación utilizando el formato:

MAJOR.MINOR.PATCH

Ejemplo:

1.0.0

Cada número representa un nivel diferente de cambio:

MAJOR.MINOR.PATCH
│     │     │
│     │     └── Correcciones y cambios pequeños
│     └──────── Nuevas funciones compatibles
└────────────── Cambios grandes o incompatibles

El agente debe analizar los cambios realizados en el proyecto y determinar qué parte de la versión debe incrementarse.

---

## 1. Estructura de una versión

Una versión estable tiene la estructura:

MAJOR.MINOR.PATCH

Ejemplos válidos:

1.0.0
1.0.1
1.1.0
1.2.3
2.0.0
2.10.4
10.3.15

Los números NO están limitados a un solo dígito.

Por ejemplo:

1.8.0
1.9.0
1.10.0
1.11.0
1.12.0

Por lo tanto, nunca asumir que después de 1.9.0 debe aparecer 2.0.0.

---

## 2. PATCH — tercer número

Formato:

1.0.X

PATCH debe incrementarse cuando el cambio no introduce una nueva funcionalidad importante y no rompe compatibilidad.

Ejemplo:

1.0.0 → 1.0.1

Utilizar PATCH para:

* Corrección de errores.
* Corrección de crashes.
* Corrección de textos.
* Ajustes pequeños de interfaz.
* Mejoras menores de rendimiento.
* Correcciones de validaciones.
* Ajustes de estilos.
* Correcciones de traducciones.
* Correcciones de compatibilidad.
* Cambios internos que no modifican el comportamiento esperado de la aplicación.
* Correcciones de seguridad compatibles.
* Refactorizaciones sin cambios funcionales visibles.

Ejemplo:

La aplicación tiene un botón "Guardar" que no funciona correctamente.

Se corrige el problema.

Antes:

1.4.2

Después:

1.4.3

Otro ejemplo:

1.4.9 → 1.4.10

PATCH puede superar 9.

---

## 3. MINOR — segundo número

Formato:

1.X.0

MINOR debe incrementarse cuando se agregan nuevas funcionalidades manteniendo compatibilidad con la versión actual.

Ejemplo:

1.2.4 → 1.3.0

Cuando MINOR aumenta, PATCH vuelve a 0.

Utilizar MINOR para:

* Nueva pantalla.
* Nueva función.
* Nuevo módulo.
* Nueva configuración.
* Nuevo sistema de notificaciones.
* Nuevo filtro.
* Nueva opción para el usuario.
* Nueva integración compatible.
* Nuevos endpoints compatibles.
* Nuevas capacidades sin eliminar las anteriores.
* Mejoras importantes de UX.
* Funcionalidades nuevas que no requieren una migración incompatible.

Ejemplo:

La aplicación actualmente permite:

* iniciar sesión;
* crear una cuenta;
* editar el perfil.

Se agrega una nueva función:

* inicio de sesión con Google.

Antes:

1.3.7

Después:

1.4.0

No debe convertirse en:

1.3.8

porque se agregó una funcionalidad nueva.

Tampoco debe convertirse automáticamente en:

2.0.0

porque la funcionalidad anterior sigue siendo compatible.

---

## 4. MAJOR — primer número

Formato:

X.0.0

MAJOR representa cambios importantes que pueden romper compatibilidad o modificar de forma sustancial el funcionamiento del producto.

Ejemplo:

1.8.4 → 2.0.0

Cuando MAJOR aumenta:

MINOR = 0
PATCH = 0

Utilizar MAJOR para:

* Cambios incompatibles.
* Rediseño completo de arquitectura pública.
* Eliminación de funciones importantes.
* Cambio incompatible de API.
* Cambio incompatible del formato de datos.
* Migraciones obligatorias.
* Reestructuración importante del producto.
* Cambio del comportamiento principal esperado por usuarios o integraciones.
* Nueva generación importante de la aplicación.

Ejemplo:

La aplicación utilizaba:

API v1

y todos los clientes dependían de sus endpoints.

Se reemplaza por una nueva API incompatible.

Antes:

1.9.8

Después:

2.0.0

---

## 5. Regla principal para elegir versión

El agente debe clasificar los cambios realizados.

Prioridad:

MAJOR > MINOR > PATCH

Si existe al menos un cambio MAJOR:

incrementar MAJOR.

Si no existe MAJOR pero existe al menos un cambio MINOR:

incrementar MINOR.

Si solamente existen cambios PATCH:

incrementar PATCH.

Ejemplo:

Versión actual:

1.6.4

Cambios realizados:

* corregir un crash → PATCH
* corregir un texto → PATCH
* agregar modo oscuro → MINOR
* mejorar rendimiento → PATCH

El cambio de mayor nivel es MINOR.

Nueva versión:

1.7.0

NO:

1.6.5

---

## 6. Reinicio de números

PATCH:

1.2.5 → 1.2.6

MINOR:

1.2.5 → 1.3.0

MAJOR:

1.2.5 → 2.0.0

Por lo tanto:

PATCH:
MAJOR.MINOR.(PATCH + 1)

MINOR:
MAJOR.(MINOR + 1).0

MAJOR:
(MAJOR + 1).0.0

---

## 7. Versiones 0.x.x

Antes de considerar una aplicación estable puede utilizarse:

0.x.x

Ejemplo:

0.1.0
0.2.0
0.3.0
0.3.1
0.4.0

Una versión 0.x normalmente representa software todavía en desarrollo y cuya interfaz puede cambiar con frecuencia.

Ejemplo de ciclo:

0.1.0
↓
0.2.0
↓
0.2.1
↓
0.3.0
↓
0.5.0
↓
0.9.0
↓
1.0.0

1.0.0 representa la primera versión pública considerada estable según la política del proyecto.

---

## 8. Alpha, Beta y Release Candidate

Cuando sea necesario distinguir versiones de desarrollo se pueden utilizar identificadores adicionales.

Ejemplos:

1.0.0-alpha.1
1.0.0-alpha.2
1.0.0-beta.1
1.0.0-beta.2
1.0.0-rc.1
1.0.0

Interpretación:

ALPHA

Versión temprana.

Puede tener:

* funciones incompletas;
* errores importantes;
* cambios frecuentes.

Ejemplo:

2.0.0-alpha.1

BETA

La mayoría de funciones principales están implementadas, pero todavía se realizan pruebas y correcciones.

Ejemplo:

2.0.0-beta.1

RC

Release Candidate.

Versión candidata a convertirse en estable si no aparecen problemas importantes.

Ejemplo:

2.0.0-rc.1

ESTABLE

Versión final:

2.0.0

Flujo recomendado:

2.0.0-alpha.1
→ 2.0.0-alpha.2
→ 2.0.0-beta.1
→ 2.0.0-beta.2
→ 2.0.0-rc.1
→ 2.0.0-rc.2
→ 2.0.0

---

## 9. No incrementar versión por cada modificación

El agente NO debe incrementar automáticamente la versión después de cada archivo modificado.

Una versión representa un conjunto de cambios destinado a una nueva publicación.

Ejemplo:

Versión publicada:

1.5.0

Durante el desarrollo se realizan 17 commits:

* corrección de login;
* cambios visuales;
* optimización;
* nueva pantalla;
* nuevas configuraciones;
* correcciones adicionales.

No hacer:

1.5.1
1.5.2
1.5.3
...
1.5.17

si esos cambios forman parte de la misma futura publicación.

El agente debe analizar el conjunto completo de cambios desde la última versión publicada.

Si el cambio más importante es una nueva funcionalidad:

1.5.0 → 1.6.0

---

## 10. Git y tags

Cuando el proyecto utiliza Git, cada versión estable debería poder asociarse con un tag.

Ejemplo:

v1.0.0
v1.1.0
v1.1.1
v1.2.0
v2.0.0

El prefijo "v" puede utilizarse en Git:

v1.4.2

mientras que internamente la aplicación puede mantener:

1.4.2

El agente debe respetar la convención existente del proyecto.

No cambiar de:

1.4.2

a:

v1.4.3

dentro de archivos internos si el proyecto históricamente no utiliza "v".

---

## 11. CHANGELOG

Cada nueva versión debería tener un resumen de cambios.

Formato recomendado:

## 1.4.0

### Added

* Nueva pantalla de configuración.
* Inicio de sesión con Google.

### Fixed

* Corregido cierre inesperado durante el login.
* Corregido problema visual del menú.

### Changed

* Mejorado rendimiento de carga.

### Security

* Actualizada validación de sesiones.

El CHANGELOG debe describir cambios útiles para humanos.

No llenar el CHANGELOG con modificaciones internas irrelevantes.

---

## 12. Detección automática de la versión actual

Antes de proponer una nueva versión, el agente debe buscar dónde está definida.

Dependiendo del proyecto puede aparecer en:

package.json
pyproject.toml
Cargo.toml
build.gradle
build.gradle.kts
pubspec.yaml
composer.json
pom.xml
AssemblyInfo
Info.plist
AndroidManifest.xml
gradle.properties

También puede existir un archivo propio:

VERSION
version.txt

El agente debe detectar primero cuál es la fuente oficial de versión del proyecto.

No modificar múltiples archivos indiscriminadamente.

Si existen varias referencias de versión, identificar cuál es la fuente principal y cuáles se generan o sincronizan desde ella.

---

## 13. Android: versionName y versionCode

En Android pueden existir dos valores diferentes.

Ejemplo:

versionName = "1.4.2"
versionCode = 37

versionName:

Es la versión visible para el usuario.

versionCode:

Es un número interno utilizado para identificar builds.

Ejemplo:

versionName 1.4.2
versionCode 37

Nueva publicación:

versionName 1.4.3
versionCode 38

El versionCode siempre debe aumentar para una publicación nueva.

No debe reutilizarse un versionCode que ya haya sido publicado.

---

## 14. Builds internos

Cuando se generan builds que no representan una nueva versión pública, se puede mantener la versión principal y utilizar metadata o identificadores internos.

Ejemplo:

1.4.0+build.52

o, dependiendo del ecosistema:

1.4.0-dev.52

No asumir que todos los gestores de paquetes o tiendas aceptan exactamente el mismo formato.

El agente debe respetar las restricciones de la plataforma utilizada.

---

## 15. Breaking Changes

Antes de aumentar MAJOR, verificar si realmente existe incompatibilidad.

Ejemplos:

Eliminar:

getUser()

y reemplazarlo obligatoriamente por:

getCurrentUser()

puede ser un breaking change si otros componentes dependen de getUser().

Otro ejemplo:

Antes:

login(email, password)

Después:

login(credentialsObject)

Si código externo utiliza la interfaz anterior, puede tratarse como breaking change.

Entonces:

3.4.1 → 4.0.0

No utilizar MAJOR simplemente porque una actualización tenga muchos archivos modificados.

El tamaño del cambio no determina por sí solo el nivel de versión.

Lo determina principalmente su impacto y compatibilidad.

---

## 16. Cambios visuales

Los cambios visuales deben evaluarse por impacto.

Ejemplo pequeño:

* corregir margen;
* cambiar espaciado;
* corregir alineación.

Normalmente:

PATCH

Ejemplo funcional:

* nueva navegación;
* nueva pantalla;
* nuevo sistema de temas.

Normalmente:

MINOR

Rediseño total que cambia significativamente el funcionamiento o elimina comportamientos anteriores:

puede justificar MAJOR.

No considerar automáticamente cualquier rediseño como MAJOR.

---

## 17. Dependencias

Actualizar una dependencia no determina automáticamente la versión.

Ejemplo:

React 20.1 → 20.2

Si la aplicación sigue funcionando igual y solo contiene correcciones internas:

PATCH

Si la actualización permite introducir una nueva funcionalidad:

MINOR

Si obliga a introducir cambios incompatibles en la interfaz pública del proyecto:

MAJOR

Analizar el efecto sobre el producto, no solamente el número de versión de la dependencia.

---

## 18. Seguridad

Una corrección de seguridad compatible normalmente debe ser:

PATCH

Ejemplo:

2.4.3 → 2.4.4

Si la solución requiere modificar interfaces públicas de manera incompatible:

MAJOR

Ejemplo:

2.4.3 → 3.0.0

---

## 19. Algoritmo de decisión

Cuando se solicite:

"actualiza la versión"
"prepara una release"
"qué versión debería ser"
"bump version"
"crea una nueva versión"

el agente debe ejecutar este procedimiento:

PASO 1

Detectar la versión actual.

PASO 2

Identificar la última versión publicada o tag cuando Git esté disponible.

PASO 3

Analizar los cambios realizados desde esa versión.

PASO 4

Clasificar cada cambio como:

PATCH
MINOR
MAJOR

PASO 5

Elegir el cambio de mayor impacto.

PASO 6

Calcular la nueva versión.

PASO 7

Mostrar la propuesta antes de realizar acciones destructivas o publicar.

Ejemplo:

Versión actual:
1.8.3

Cambios detectados:

* corrección del login → PATCH
* optimización de imágenes → PATCH
* nueva pantalla de estadísticas → MINOR

Resultado:

MINOR

Versión propuesta:

1.9.0

Motivo:

Se agregó una nueva funcionalidad compatible.

---

## 20. Tabla mental de decisión

Usar esta lógica:

¿Rompe compatibilidad?
|
├── Sí → MAJOR
|
└── No
|
¿Agrega funcionalidad?
|
├── Sí → MINOR
|
└── No
|
¿Corrige o mejora algo existente?
|
├── Sí → PATCH
|
└── No → probablemente no requiere nueva versión

---

## 21. Ejemplos completos

CASO A

Actual:

1.0.0

Cambio:

Corregir botón que no responde.

Resultado:

1.0.1

---

CASO B

Actual:

1.0.8

Cambio:

Agregar sistema de favoritos.

Resultado:

1.1.0

---

CASO C

Actual:

1.9.7

Cambio:

Agregar otra función compatible.

Resultado:

1.10.0

NO:

2.0.0

---

CASO D

Actual:

1.10.6

Cambio:

Eliminar una API utilizada públicamente.

Resultado:

2.0.0

---

CASO E

Actual:

2.5.9

Cambios:

* corregir 8 bugs;
* mejorar rendimiento;
* corregir textos.

Resultado:

2.5.10

No incrementar una vez por cada bug.

---

CASO F

Actual:

2.5.9

Cambios:

* corregir 8 bugs;
* agregar buscador avanzado;
* agregar filtros.

Resultado:

2.6.0

MINOR tiene prioridad sobre PATCH.

---

CASO G

Actual:

2.5.9

Cambios:

* 20 correcciones;
* 5 funciones nuevas;
* eliminar API anterior incompatible.

Resultado:

3.0.0

MAJOR tiene prioridad sobre todos los demás cambios.

---

## 22. Reglas que el agente NO debe romper

NUNCA incrementar MAJOR simplemente porque MINOR llegó a 9.

Incorrecto:

1.9.0 → 2.0.0

si solamente se agregó otra funcionalidad compatible.

Correcto:

1.9.0 → 1.10.0

NUNCA utilizar cantidad de commits como número PATCH.

NUNCA incrementar versión solamente porque se recompiló el proyecto.

NUNCA reducir una versión publicada.

NUNCA reutilizar una versión publicada para una release diferente.

NUNCA cambiar una versión histórica.

NUNCA sobrescribir un tag publicado sin una razón explícita y controlada.

NUNCA asumir que una actualización grande visualmente significa MAJOR.

NUNCA publicar automáticamente una release, package o tag remoto sin autorización cuando la operación tenga efectos externos.

---

## 23. Comportamiento esperado del agente

Cuando el usuario solicite preparar una nueva versión, responder inicialmente con información similar a:

Versión actual: 1.7.4

Cambios detectados:

* 3 correcciones → PATCH
* 1 nueva funcionalidad → MINOR

Nivel recomendado: MINOR

Nueva versión: 1.8.0

Archivos que requieren actualización:

* package.json
* CHANGELOG.md

Después puede realizar las modificaciones correspondientes si el usuario solicitó explícitamente aplicarlas.

---

## 24. Regla de seguridad

Separar siempre estas operaciones:

1. Calcular versión.
2. Modificar archivos locales.
3. Crear commit.
4. Crear tag.
5. Hacer push.
6. Crear release.
7. Publicar en tienda, registro o servidor.

Que el usuario solicite:

"actualiza la versión"

no significa automáticamente:

"publica la aplicación".

Las acciones externas deben respetar el alcance exacto solicitado.

---

## 25. Regla final

El objetivo del versionado no es contar cuántas veces se modificó el código.

El objetivo es comunicar qué significa una nueva versión respecto a la anterior.

Regla resumida:

PATCH = arreglé algo.

MINOR = agregué algo compatible.

MAJOR = cambié algo de manera incompatible.

Ejemplos:

1.0.0 → 1.0.1 = corrección

1.0.1 → 1.1.0 = nueva funcionalidad

1.1.0 → 2.0.0 = cambio incompatible

Siempre analizar primero el impacto real de los cambios antes de decidir la siguiente versión.
