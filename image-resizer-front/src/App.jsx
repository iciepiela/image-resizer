import './App.css';
import ImageGrid from './pages/ImageGrid';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast'

function App() {
  return (
    <BrowserRouter>
      <Toaster position='bottom-right' toastOptions={{ duration: 2000 }} />
      <Routes>
        <Route path="/" element={<ImageGrid />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
