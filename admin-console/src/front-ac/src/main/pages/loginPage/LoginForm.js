import React, { useState } from 'react';
import './LoginPage.css';
import axios from "../../../utils/axios";
import {setAuth} from "../../../utils/auth";
import imageSrc from './ib.png'; // Import the image file

const LoginPage = ({setLoggedIn}) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const handleEmailChange = (e) => {
        setEmail(e.target.value);
    };

    const handlePasswordChange = (e) => {
        setPassword(e.target.value);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        // Handle login logic here

        const response = await axios.post("http://localhost:8080/authenticate", {
            username: email,
            password: password
        })
        if (response?.data?.jwtToken) {
            setLoggedIn(true)
            setAuth(response?.data?.jwtToken)
        } else {
            setLoggedIn(false);
        }
        // then((response) => {
        //     console.log("hoii")
        //     setLoggedIn(true);
        //     console.log(response)
        //     setAuth(response.jwtToken)
        // }).catch(() => {
        //     setLoggedIn(false)
        // })


        setEmail('');
        setPassword('');
    };

    return (
        <div className="login-page">
            <div className="login-container">
                <img src={imageSrc} alt="abgd" width={200} />
                <br/>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>მომხმარებლის სახელი</label>
                        <input
                            type="text"
                            placeholder="შეიყვანეთ მომხმარებლის სახელი"
                            value={email}
                            onChange={handleEmailChange}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>პაროლი</label>
                        <input
                            type="password"
                            placeholder="შეიყვანეთ პაროლი"
                            value={password}
                            onChange={handlePasswordChange}
                            required
                        />
                    </div>
                    <button type="submit">შესვლა</button>
                </form>
            </div>
        </div>
    );
};

export default LoginPage;
