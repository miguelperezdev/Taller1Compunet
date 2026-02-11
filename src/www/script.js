// script.js - Versión Mínima
document.addEventListener('DOMContentLoaded', () => {
    // Fecha
    const dateEl = document.getElementById('currentDate');
    if (dateEl) {
        dateEl.textContent = new Date().toLocaleDateString('es-ES');
    }

    // Terminal básica
    const termOutput = document.getElementById('terminalOutput');
    const messages = ["Sistema activo", "Conexión estable", "Listo"];

    if (termOutput) {
        messages.forEach((msg, i) => {
            setTimeout(() => {
                const line = document.createElement('div');
                line.className = 'terminal-line';
                line.textContent = msg;
                termOutput.appendChild(line);
            }, i * 1000);
        });
    }

    // Efectos imágenes
    document.querySelectorAll('.hack-image').forEach(img => {
        img.onmouseenter = () => img.style.transform = 'scale(1.05)';
        img.onmouseleave = () => img.style.transform = 'scale(1)';
    });

    // Estado dinámico
    setInterval(() => {
        const conn = document.getElementById('connections');
        if (conn) {
            const random = Math.floor(Math.random() * 5) + 22;
            conn.textContent = random + ' ACT';
        }
    }, 5000);
});

// Función para botones de comandos
function runCommand(cmd) {
    alert('Ejecutando: ' + cmd + '\n✅ Completado');
}