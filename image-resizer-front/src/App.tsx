import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import ImageGrid from "./pages/ImageGrid.tsx";
import { Toaster } from "react-hot-toast";

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ImageGrid />} />
      </Routes>
      <Toaster />
    </BrowserRouter>
  );
};

export default App;
