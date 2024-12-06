import React, { useState } from 'react';
import { fromEvent, takeWhile } from 'rxjs';
import { map } from 'rxjs/operators';
import { extractZip } from '../ImageUtils';

const ImageGrid = () => {
    const [images, setImages] = useState([]);
    const COMPLETE_REQUEST="COMPLETE_REQUEST"

    const loadPhotos = () => {
        console.log("Loading photos...")
        setImages([])
        const eventSource = new EventSource('http://localhost:8080/images/original');

        const imageStream$ = fromEvent(eventSource, 'message').pipe(
            map((event) => {
                const parsedData = JSON.parse(event.data)
                return parsedData.body;
            }),
            takeWhile((imageData) => imageData.name !== COMPLETE_REQUEST || imageData.base64 !== COMPLETE_REQUEST, true)
        );

        const subscription = imageStream$.subscribe({
            next: (imageData) => {
                if (imageData.name === COMPLETE_REQUEST && imageData.base64 === COMPLETE_REQUEST) {
                    console.log("ended");
                    eventSource.close();
                } else {
                    console.log(imageData)
                    setImages((prevImages) => [...prevImages, { base64: `data:image/png;base64,${imageData.base64}`, name: "abc" }]);
                }

            },
            error: (err) => {
                console.error('Error receiving image data:', err);
            },
            complete: () => { console.log("Complete") }
        });


        return () => {
            eventSource.close();
            subscription.unsubscribe();
        };
    };

    const uploadPhotos = (imageList) => {
        console.log("upload")
        var preparedImages = imageList.map(image => { return { name: image.name, base64: image.base64 } });
        uploadImagesToBackend(preparedImages)
            .then(result => result ? loadPhotos() : console.log("Error! The photos where not uploaded!"))
    };

    const uploadImagesToBackend = async (image) => {
        try {
            const response = await fetch('http://localhost:8080/images/upload', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify([{ name: image.name, base64: image.base64 }])
            });

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            const responseBody = await response.json();
            console.log("Photos uploaded succesfully: ", responseBody);
            return responseBody;
        } catch (error) {
            console.error('Error during fetch:', error);
            throw error;
        }
    };

    const handleFileChange = (event) => {
        const file = event.target.files[0];
        if (file && file.name.endsWith('.zip')) {
            extractZip(file)
            .then(loadedImgs=>{
                setImages(loadedImgs);
                uploadPhotos(loadedImgs);

            })
        } else {
            alert('Please upload a valid ZIP file.');
        }
    };
    

    return (
        <div>
            <button onClick={loadPhotos}>Load</button>
            <input type="file" onChange={handleFileChange} />
            {images.map((image) => (
                // image.loaded ?
                    <div key={image.key} style={{ margin: '10px', display: 'inline-block' }}>
                        <img
                            src={`${image.base64}`}
                            alt={`Image ${image.name}`}
                            style={{ width: '200px', height: 'auto', borderRadius: '8px' }}
                        />
                    </div>
                    // : <div>
                    //     Loading image
                    // </div>
            ))}
        </div>
    );
};
export default ImageGrid;
