document.getElementById("uploadForm").addEventListener("submit", function(e) {
  e.preventDefault();

  const formData = new FormData();
  const archivo = document.getElementById("archivo").files[0];
  formData.append("archivo", archivo);

  fetch("http://localhost:8080/procesar-pdf", {
    method: "POST",
    body: formData
  })
  .then(res => res.text())
  .then(data => {
    document.getElementById("respuesta").textContent = data;
    const utterance = new SpeechSynthesisUtterance(data);
    speechSynthesis.speak(utterance);
  })
  .catch(err => {
    console.error(err);
    alert("Ocurrió un error al enviar el archivo.");
  });
});
