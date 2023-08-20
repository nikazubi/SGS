import Button from '@mui/material/Button';
import {Link} from 'react-router-dom'
const Login = () => {

    return (
        <div className='login__wrapper'>
        <div className="loginCnt" style={{textAlign:'center'}}>
            <div className="loginCnt__img">
                {/* <img src={imageSrc} alt="IB Mtiebi Logo"/> */}
            </div>
                <div className="loginCnt__circle blue">
                    <div className="formCnt">
                        <div className="formCnt__title">ავტორიზაცია</div>
                        <form onSubmit={console.log('handleSubmit')}>
                            <div className="login__inputCnt">
                            <label className="login__label" htmlFor="login_input">ელ.ფოსტა:</label>
                            <input class="formCnt__input" id="login_input" /*value={email}*/ onChange={()=>console.log('handleEmailChange')} type="text"/>
                            </div>
                            <div className="login__inputCnt">
                                <label className="login__label" htmlFor="login_password">პაროლი:</label>
                                <input className="formCnt__input" id="login_password" /*value={password} */ onChange={console.log('handlePasswordChange')}
                                    type="password"/>
                                <div className='remember__password'>
                                    <Link to="#">დაგავიწყდა პაროლი?</Link>
                                </div>
                            </div>

                            <div className="formCnt__btnCnt">
                                <Button variant="contained">შესვლა</Button>
                            </div>
                        </form>
                    </div>
                </div>
        </div>
        </div>
    )
}

export default Login;