import "./App.css";
import ImageGrid from "./pages/ImageGrid";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Toaster } from "react-hot-toast";

function App() {
  return (
    <BrowserRouter>
      <ImageGrid />
    </BrowserRouter>
  );
}

export default App;
