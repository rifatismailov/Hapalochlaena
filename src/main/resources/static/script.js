const dropArea = document.querySelector(".drop_box"),
    button = dropArea.querySelector("button"),
    input = dropArea.querySelector("input");

button.onclick = () => {
    input.click();
};

input.addEventListener("change", function (e) {
    const file = e.target.files[0];
    if (!file) return;

    const fileName = file.name;
    const fileForm = `
    <form class="form" method="post" action="/upload" enctype="multipart/form-data">
      <h4>${fileName}</h4>
      <input type="email" name="email" placeholder="Enter email to upload file" required>
      <input type="hidden" name="fileName" value="${fileName}">
      <button class="btn" type="submit">Upload</button>
    </form>`;

    dropArea.innerHTML = fileForm;
});
